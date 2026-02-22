(ns quadvote.cqrs
  (:require
    [tada.events.core :as tada]
    [dat.api :as dat]
    [sys.api :as sys]
    [quadvote.state :as state]
    [quadvote.model :as model]))

(defn entity-exists?-condition
  [key value]
  [#(state/entity-exists? key value)
   :invalid (str "Entity with " [key value] " does not exist.")])

(defn user-is-admin-of-group?-condition
  [user-id group-id]
  [#(state/user-is-admin-of-group? user-id group-id)
   :forbidden "User is not an admin of this group."])

(defn user-is-member-of-group?-condition
  [user-id group-id]
  [#(state/user-is-member-of-group? user-id group-id)
   :forbidden "User is not an member of this group."])

(defn topic-is-within-group?-condition
  [topic-id group-id]
  [#(state/topic-is-within-group? topic-id group-id)
   :forbidden "Topic is not within of this group."])

(def queries
  [{:id :api/user
    :params {:user-id :user/id}
    :return
    (fn [{:keys [user-id]}]
      (dat/q
       '[:find (pull ?user [:user/id
                            :user/name
                            {:membership/_user
                             [:membership/id
                              {:membership/group [:group/id
                                                  :group/name]} ]}]) .
         :in $ ?user-id
         :where
         [?user :user/id ?user-id]]
       @(state/conn)
       user-id))}

   {:id :api/membership
    :params {:group-id :group/id
             :user-id :user/id}
    :conditions
    (fn [{:keys [user-id group-id]}]
      [(entity-exists?-condition :user/id user-id)
       (entity-exists?-condition :group/id group-id)
       (user-is-member-of-group?-condition user-id group-id)])
    :return
    (fn [{:keys [group-id user-id]}]
      (dat/q
       '[:find (pull ?membership [:membership/id
                                  :membership/admin?
                                  :membership/balance
                                  :membership/claimable-token-amount
                                  {:membership/group
                                   [:group/id
                                    :group/name
                                    :group/description
                                    :group/balance
                                    :group/open-membership?
                                    :group/open-topics?
                                    :group/grant-frequency
                                    :group/grant-amount
                                    {:topic/_group
                                     [:topic/id
                                      :topic/title
                                      :topic/description
                                      {:topic/burn
                                       [:burn/id
                                        :burn/message
                                        :burn/timestamp
                                        {:burn/user
                                         [:user/id
                                          :user/name]}]}
                                      {:topic/user
                                       [:user/id
                                        :user/name]}
                                      {:vote/_topic
                                       [:vote/id
                                        :vote/voice-amount
                                        {:vote/user [:user/id
                                                     :user/name]}]}]}]}]) .
         :in $ ?group-id ?user-id
         :where
         [?group :group/id ?group-id]
         [?user :user/id ?user-id]
         [?membership :membership/group ?group]
         [?membership :membership/user ?user]]
       @(state/conn)
       group-id
       user-id))}])

(def commands
  [

   {:id :api/vote!
    :params {:topic-id :topic/id
             :voice-amount :vote/voice-amount
             :user-id :user/id}
    :conditions
    (fn [{:keys [topic-id voice-amount user-id]}]
      (let [group-id (dat/q '[:find ?group-id .
                              :in $ ?topic-id
                              :where
                              [?t :topic/id ?topic-id]
                              [?t :topic/group ?g]
                              [?g :group/id ?group-id]]
                            @(state/conn)
                            topic-id)]
        [(entity-exists?-condition :user/id user-id)
         (entity-exists?-condition :topic/id topic-id)
         (user-is-member-of-group?-condition user-id group-id)
         [#(state/user-can-afford? {:user-id user-id
                                    :group-id group-id
                                    :topic-id topic-id
                                    :voice-amount voice-amount})
          :invalid "User has insufficient tokens to upvote"]]))
    :effect
    (fn [{:keys [topic-id voice-amount user-id]}]
      (let [[vote-id previous-amount] (first (dat/q '[:find ?vote-id ?previous-amount
                                                      :in $ ?user-id ?topic-id
                                                      :where
                                                      [?u :user/id ?user-id]
                                                      [?t :topic/id ?topic-id]
                                                      [?v :vote/user ?u]
                                                      [?v :vote/topic ?t]
                                                      [?v :vote/id ?vote-id]
                                                      [?v :vote/voice-amount ?previous-amount]]
                                                    @(state/conn)
                                                    user-id
                                                    topic-id))
            [membership-id balance] (first (dat/q '[:find ?membership-id ?balance
                                                    :in $ ?user-id ?topic-id
                                                    :where
                                                    [?u :user/id ?user-id]
                                                    [?t :topic/id ?topic-id]
                                                    [?t :topic/group ?g]
                                                    [?g :group/id ?group-id]
                                                    [?m :membership/user ?u]
                                                    [?m :membership/group ?g]
                                                    [?m :membership/id ?membership-id]
                                                    [?m :membership/balance ?balance]]
                                                  @(state/conn)
                                                  user-id
                                                  topic-id))]
        (dat/transact!
         (state/conn)
         [(if (= 0 voice-amount)
            [:db/retractEntity [:vote/id vote-id]]
            {:vote/id (or vote-id (dat/uuid))
             :vote/topic [:topic/id topic-id]
             :vote/user [:user/id user-id]
             :vote/voice-amount voice-amount})
          {:membership/id membership-id
           :membership/balance (let [delta (model/token-cost
                                            (or previous-amount 0)
                                            voice-amount)]
                                 (- balance delta))}])))}

   {:id :api/create-group!
    :params {:name :group/name
             :user-id :user/id}
    :conditions
    (fn [{:keys [user-id]}]
      [(entity-exists?-condition :user/id user-id)])
    :effect
    (fn [{:keys [name user-id]}]
      (dat/transact!
        (state/conn)
        [{:db/id -1
          :group/id (dat/uuid)
          :group/name name
          :group/description ""
          :group/open-membership? false
          :group/open-topics? false}
         {:membership/id (dat/uuid)
          :membership/user [:user/id user-id]
          :membership/balance 25
          :membership/claimable-token-amount 25
          :membership/group -1
          :membership/admin? true}]))}

   ;; admin-only

   {:id :api/add-user-to-group!
    :params {:name :user/name
             :email :user/email
             :user-id :user/id
             :group-id :group/id}
    :conditions
    (fn [{:keys [user-id group-id email]}]
      [(entity-exists?-condition :user/id user-id)
       (entity-exists?-condition :group/id group-id)
       (user-is-admin-of-group?-condition user-id group-id)
       [#(let [new-user-id (state/email->user-id email)]
           (not (state/user-is-member-of-group? new-user-id group-id)))
        :forbidden "User is already a member of this group."]])
    :effect
    (fn [{:keys [name group-id email]}]
      (dat/transact! (state/conn)
                     (if-let [user-id (state/email->user-id email)]
                       [{:membership/id (dat/uuid)
                         :membership/user [:user/id user-id]
                         :membership/claimable-token-amount 25
                         :membership/balance 0
                         :membership/group [:group/id group-id]}]
                       [{:db/id -1
                         :user/id (dat/uuid)
                         :user/name name
                         :user/email email}
                        {:membership/id (dat/uuid)
                         :membership/claimable-token-amount 25
                         :membership/balance 0
                         :membership/user -1
                         :membership/group [:group/id group-id]}])))}

   {:id :api/create-topic!
    :params {:title :topic/title
             :description :topic/description
             :group-id :group/id
             :user-id :user/id}
    :conditions
    (fn [{:keys [user-id group-id]}]
      [(entity-exists?-condition :user/id user-id)
       (entity-exists?-condition :group/id group-id)
       (user-is-member-of-group?-condition user-id group-id)
       [#(or (state/user-is-admin-of-group? user-id group-id)
             (state/eav [:group/id group-id] :group/open-topics?))
        :forbidden "Topic creation is restricted to admins."]])
    :effect
    (fn [{:keys [title description group-id user-id]}]
      (dat/transact! (state/conn)
                     [{:topic/id (dat/uuid)
                       :topic/group [:group/id group-id]
                       :topic/user [:user/id user-id]
                       :topic/title title
                       :topic/description description}]))}

   {:id :api/burn-topic!
    :params {:topic-id :topic/id
             :message :burn/message
             :user-id :user/id}
    :conditions
    (fn [{:keys [user-id topic-id]}]
      [(entity-exists?-condition :user/id user-id)
       (entity-exists?-condition :topic/id topic-id)
       [(fn []
          (let [group-id (dat/q '[:find ?group-id .
                                  :in $ ?topic-id
                                  :where
                                  [?t :topic/id ?topic-id]
                                  [?t :topic/group ?g]
                                  [?g :group/id ?group-id]]
                                @(state/conn)
                                topic-id)]
            (user-is-admin-of-group?-condition user-id group-id)))
        :forbidden "User is not an admin of the group that owns this topic."]])
    :effect
    (fn [{:keys [user-id topic-id message]}]
      (dat/transact! (state/conn)
                     [{:topic/id topic-id
                       :topic/burn {:burn/id (dat/uuid)
                                    :burn/message message
                                    :burn/user [:user/id user-id]
                                    :burn/timestamp (java.util.Date.)}}]))}

   {:id :api/update-group!
    :params [:map
             [:user-id :user/id]
             [:group-id :group/id]
             [:name {:optional true} :group/name]
             [:description {:optional true} :group/description]
             [:open-membership? {:optional true} :group/open-membership?]
             [:open-topics? {:optional true} :group/open-topics?]
             [:grant-frequency {:optional true} :group/grant-frequency]
             [:grant-amount {:optional true} :group/grant-amount]]
    :conditions
    (fn [{:keys [user-id group-id]}]
      [(entity-exists?-condition :user/id user-id)
       (entity-exists?-condition :group/id group-id)
       (user-is-admin-of-group?-condition user-id group-id)])
    :effect
    (fn [{:keys [group-id name description open-membership? open-topics? grant-frequency grant-amount]}]
      (dat/transact! (state/conn)
                     [(->> {:group/id group-id
                            :group/name name
                            :group/description description
                            :group/open-membership? open-membership?
                            :group/open-topics? open-topics?
                            :group/grant-frequency grant-frequency
                            :group/grant-amount grant-amount}
                           (filter (comp some? val))
                           (into {}))]))}

   {:id :api/claim!
    :params {:membership-id :membership/id
             :user-id :user/id}
    :conditions
    (fn [{:keys [membership-id user-id]}]
      [(entity-exists?-condition :user/id user-id)
       (entity-exists?-condition :membership/id membership-id)
       [#(boolean (dat/q '[:find ?user-id .
                           :in $ ?user-id ?membership-id
                           :where
                           [?u :user/id ?user-id]
                           [?m :membership/id ?membership-id]
                           [?m :membership/user ?u]]
                         @(state/conn)
                         user-id
                         membership-id))
        :forbidden "This membership does not belong to this user"]])
    :effect
    (fn [{:keys [membership-id]}]
      ;; race condition - querying seperately from transaction
      (let [[balance claimable-token-amount]
            (dat/q '[:find [?balance ?claimable]
                     :in $ ?membership-id
                     :where
                     [?m :membership/id ?membership-id]
                     [?m :membership/balance ?balance]
                     [?m :membership/claimable-token-amount ?claimable]]
                   @(state/conn)
                   membership-id)]
        (dat/transact! (state/conn)
                       [{:membership/id membership-id
                         :membership/claimable-token-amount 0
                         :membership/balance (+ balance
                                                claimable-token-amount)}
                        {:claim/id (dat/uuid)
                         :claim/membership [:membership/id membership-id]
                         :claim/timestamp (java.util.Date.)}])))}])


(def component
  {:sys.component/id :tada
   :sys.component/expects #{:conn}
   :sys.component/provides #{:tada}
   :sys.component/start (fn [_]
                          (let [t
                                (tada/init :malli)]

                            (tada/register! t
                                            (concat queries
                                                    commands))
                            {:tada t}))
   :sys.component/stop (fn [{:keys [tada]}]
                         (reset! (:tada/event-store tada) {}))})

(defn registry
  []
  (sys/get :system :tada))

(when-let [t (sys/get :system :tada)]
  (tada/register! t
                  (concat queries
                          commands)))
