(ns quadvote.state
  (:require
   [dat.api :as dat]
   [datascript.core :as d]
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
    :membership/user {:dat/rel [:dat.rel/one
                                :entity/user
                                :user/id]}
    :membership/group {:dat/rel [:dat.rel/one
                                 :entity/group
                                 :group/id]}}

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
                           :user/id]}}
   :entity/vote
   {:vote/id {:dat/spec :uuid
              :dat/unique :dat.unique/identity}
    :vote/voice-amount {:dat/spec :int}
    :vote/topic {:dat/rel [:dat.rel/one
                           :entity/topic
                           :topic/id]}
    :vote/user {:dat/rel [:dat.rel/one
                          :entity/user
                          :user/id]}}})

(defonce conn (dat/init! :dat.db/datascript schema {:storage (d/file-storage (config/get :db-path))}))

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

(comment
  ;; all EAVs
  (dat/q '[:find ?e ?a ?v
          :where
          [?e ?a ?v]]
        @conn))
