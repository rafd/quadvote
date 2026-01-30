(ns quadvote.state
  (:require
   [dat.api :as dat]
   [quadvote.model :as model]
   [quadvote.config :as config]))

(def schema
  {:entity/group
   {:group/id {:dat/spec :uuid
               :dat/unique :dat.unique/identity}
    :group/name {:dat/spec :string}}

   :entity/user
   {:user/id {:dat/spec :uuid
              :dat/unique :dat.unique/identity}
    :user/name {:dat/spec :string}
    :user/email {:dat/spec :string}}

   :entity/membership
   {:membership/id {:dat/spec :uuid
                    :dat/unique :dat.unique/identity}
    :membership/balance {:dat/spec :int}
    :membership/admin? {:dat/spec :boolean}
    :membership/claimable-token-amount {:dat/spec :int}
    :membership/user {:dat/rel [:dat.rel/one
                                :entity/user
                                :user/id]}
    :membership/group {:dat/rel [:dat.rel/one
                                 :entity/group
                                 :group/id]}}

   :entity/claim
   {:claim/id {:dat/spec :uuid
               :dat/unique :dat.unique/identity}
    :claim/amount {:dat/spec :int}
    :claim/timestamp {:dat/spec :inst}
    :claim/membership {:dat/rel [:dat.rel/one
                                 :entity/membership
                                 :membership/id]}}

   :entity/topic
   {:topic/id {:dat/spec :uuid
               :dat/unique :dat.unique/identity}
    :topic/title {:dat/spec :string}
    :topic/description {:dat/spec :string}
    :topic/group {:dat/rel [:dat.rel/one
                            :entity/group
                            :group/id]}
    :topic/user {:dat/rel [:dat.rel/one
                           :entity/user
                           :user/id]}
    :topic/burn {:dat/rel [:dat.rel/one
                           :entity/burn
                           :burn/id]}}

   :entity/vote
   {:vote/id {:dat/spec :uuid
              :dat/unique :dat.unique/identity}
    :vote/voice-amount {:dat/spec :int}
    :vote/topic {:dat/rel [:dat.rel/one
                           :entity/topic
                           :topic/id]}
    :vote/user {:dat/rel [:dat.rel/one
                          :entity/user
                          :user/id]}}

   :entity/burn
   {:burn/id {:dat/spec :uuid
              :dat/unique :dat.unique/identity}
    :burn/message {:dat/spec :string}
    :burn/timestamp {:dat/spec :inst}
    :burn/user {:dat/rel [:dat.rel/one
                          :entity/user
                          :user/id]}}})

(defonce conn (dat/init! :dat.db/datascript schema {:file-path (config/get :db-path)}))

(defn entity-exists?
  [k v]
  (boolean (dat/q '[:find ?e .
                    :in $ ?k ?v
                    :where
                    [?e ?k ?v]]
                  @conn k v)))

(defn user-can-afford?
  [{:keys [user-id group-id topic-id voice-amount]}]
  (model/can-afford?
   {:balance
    (dat/q
     '[:find ?balance .
       :in $ ?user-id ?group-id
       :where
       [?u :user/id ?user-id]
       [?g :group/id ?group-id]
       [?m :membership/user ?u]
       [?m :membership/group ?g]
       [?m :membership/balance ?balance]]
     @conn
     user-id
     group-id)
    :old-voice-amount
    (or (dat/q
         '[:find ?voice-amount .
           :in $ ?user-id ?topic-id
           :where
           [?u :user/id ?user-id]
           [?t :topic/id ?topic-id]
           [?v :vote/user ?u]
           [?v :vote/topic ?t]
           [?v :vote/voice-amount ?voice-amount]]
         @conn
         user-id
         topic-id)
        0)
    :new-voice-amount
    voice-amount}))

(defn user-is-admin-of-group?
  [user-id group-id]
  (boolean (dat/q '[:find ?e .
                    :in $ ?user-id ?group-id
                    :where
                    [?u :user/id ?user-id]
                    [?g :group/id ?group-id]
                    [?e :membership/user ?u]
                    [?e :membership/group ?g]
                    [?e :membership/admin? true]]
                  @conn
                  user-id
                  group-id)))

(defn user-is-member-of-group?
  [user-id group-id]
  (boolean (dat/q '[:find ?e .
                    :in $ ?user-id ?group-id
                    :where
                    [?u :user/id ?user-id]
                    [?g :group/id ?group-id]
                    [?e :membership/user ?u]
                    [?e :membership/group ?g]]
                  @conn
                  user-id
                  group-id)))

(defn topic-is-within-group?
  [topic-id group-id]
  (boolean (dat/q '[:find ?t .
                    :in $ ?topic-id ?group-id
                    :where
                    [?t :topic/id ?topic-id]
                    [?g :group/id ?group-id]
                    [?t :topic/group ?g]]
                  @conn
                  topic-id
                  group-id)))

(defn email->user-id
  [email]
  (dat/q '[:find ?id .
           :in $ ?email
           :where
           [?e :user/email ?email]
           [?e :user/id ?id]]
         @conn
         email))

(defn grant-to-group!
  [group-id to-grant-amount]
  ;; race condition - querying seperate from transaction
  (let [ids-and-amounts (dat/q '[:find ?membership-id ?amount
                                 :in $ ?group-id
                                 :where
                                 [?g :group/id ?group-id]
                                 [?m :membership/group ?g]
                                 [?m :membership/id ?membership-id]
                                 [?m :membership/claimable-token-amount ?amount]]
                               @conn
                               group-id)]
    (dat/transact!
     conn
     (->> ids-and-amounts
          (map (fn [[membership-id amount]]
                 {:membership/id membership-id
                  :membership/claimable-token-amount (max amount to-grant-amount)}))))
    nil))

#_(grant-to-group!
   (dat/q '[:find ?group-id .
          :where [_ :group/id ?group-id]]
        @conn)
   25)

(comment
  ;; all EAVs
  (dat/q '[:find ?e ?a ?v
          :where
          [?e ?a ?v]]
        @conn))
