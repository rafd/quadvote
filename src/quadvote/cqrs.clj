(ns quadvote.cqrs
  (:require
    [tada.events.core :as tada]
    [dat.api :as dat]
    [quadvote.state :as state]))

(def queries
  [{:id :api/data
    :params {:user-id uuid?}
    :return
    (fn [{:keys [user-id]}]
      {:api.data/topics (dat/q
                         '[:find [(pull ?topic [:topic/id
                                                :topic/title
                                                :topic/description]) ...]
                           :where
                           [?topic :topic/id _]]
                         @state/conn)
       :api.data/topic-voice-amounts (state/votes->topic-voice-amounts
                                      (dat/q
                                       '[:find [(pull ?vote [{:vote/topic [:topic/id]}
                                                             :vote/voice-amount]) ...]
                                         :where
                                         [?vote :vote/id _]]
                                       @state/conn))
       :api.data/user (dat/q
                       '[:find (pull ?user [:user/id
                                            :user/name]) .
                         :in $ ?user-id
                         :where
                         [?user :user/id ?user-id]]
                         @state/conn
                       user-id)
       :api.data/group-id (dat/q
                          '[:find ?group-id .
                            :in $ ?user-id
                            :where
                            [?user :user/id ?user-id]
                            [?membership :membership/user ?user]
                            [?membership :membership/group ?group]
                            [?group :group/id ?group-id]]
                          @state/conn
                          user-id)
       :api.data/balance (dat/q
                          '[:find ?balance .
                            :in $ ?user-id
                            :where
                            [?user :user/id ?user-id]
                            [?membership :membership/user ?user]
                            [?membership :membership/balance ?balance]]
                          @state/conn
                          user-id)
       :api.data/votes (->> (dat/q
                             '[:find [(pull ?vote [:vote/id
                                                   {:vote/topic [:topic/id]}
                                                   :vote/voice-amount]) ...]
                               :in $ ?user-id
                               :where
                               [?user :user/id ?user-id]
                               [?vote :vote/user ?user]]
                             @state/conn
                             user-id)
                            (map (fn [vote]
                                   (-> vote
                                       (assoc :vote/topic-id (get-in vote [:vote/topic :topic/id]))
                                       (dissoc :vote/topic)))))})}])

(defn entity-exists?-condition
  [key value]
  [#(state/entity-exists? key value)
   :invalid "Entity with " [key value] " does not exist."])

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

(def commands
  [

   {:id :api/vote!
    :params {:vote-id uuid?
             :topic-id uuid?
             :group-id uuid?
             :voice-amount (fn [x]
                             (and
                               (int? x)
                               (<= 0 x 5)))
             :user-id uuid?}
    :conditions
    (fn [{:keys [topic-id voice-amount user-id group-id]}]
      [(entity-exists?-condition :user/id user-id)
       (entity-exists?-condition :topic/id topic-id)
       (topic-is-within-group?-condition topic-id group-id)
       (user-is-member-of-group?-condition user-id group-id)
       [#(state/user-can-afford? {:user-id user-id
                                  :group-id group-id
                                  :topic-id topic-id
                                  :voice-amount voice-amount})
        :invalid "User has insufficient tokens to upvote"]])
    :effect
    (fn [{:keys [topic-id voice-amount user-id]}]
      (let [vote-id (dat/q '[:find ?vote-id .
                             :in $ ?user-id ?topic-id
                             :where
                             [?u :user/id ?user-id]
                             [?t :topic/id ?topic-id]
                             [?v :vote/user ?u]
                             [?v :vote/topic ?t]
                             [?v :vote/id ?vote-id]]
                           @state/conn
                           user-id
                           topic-id)]
        (dat/transact! state/conn
         (if (= 0 voice-amount)
           [[:db/retractEntity [:vote/id vote-id]]]
           [{:vote/id (or vote-id (dat/uuid))
             :vote/topic [:topic/id topic-id]
             :vote/user [:user/id user-id]
             :vote/voice-amount voice-amount}]))))}

   {:id :api/claim-tokens!
    :params {:secret string?
             :user-id uuid?}
    :conditions
    (fn [{:keys [secret user-id]}]
      [(entity-exists?-condition :user/id user-id)])
    ;; secret exists
    ;; user hasn't claimed this secret already
    :effect
    (fn [{:keys [user-id]}]
      ;; TODO
      )}

   {:id :api/create-group!
    :params {:name string?
             :user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [(entity-exists?-condition :user/id user-id)])
    :effect
    (fn [{:keys [name user-id]}]
      (dat/transact!
        state/conn
        [{:db/id -1
          :group/id (dat/uuid)
          :group/name name}
         {:membership/id (dat/uuid)
          :membership/user [:user/id user-id]
          :membership/group -1
          :membership/admin? true}]))}

   ;; admin-only

   {:id :api/add-user-to-group!
    :params {:name string?
             :email string?
             :user-id uuid?
             :group-id uuid?}
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
      (dat/transact! state/conn
                     (if-let [user-id (state/email->user-id email)]
                       [{:membership/id (dat/uuid)
                         :membership/user [:user/id user-id]
                         :membership/group [:group/id group-id]}]
                       [{:db/id -1
                         :user/id (dat/uuid)
                         :user/name name
                         :user/email email}
                        {:membership/id (dat/uuid)
                         :membership/user -1
                         :membership/group [:group/id group-id]}])))}

   {:id :api/create-topic!
    :params {:title string?
             :description string?
             :group-id uuid?
             :user-id uuid?}
    :conditions
    (fn [{:keys [user-id group-id]}]
      [(entity-exists?-condition :user/id user-id)
       (entity-exists?-condition :group/id group-id)
       (user-is-admin-of-group?-condition user-id group-id)])
    :effect
    (fn [{:keys [title description group-id user-id]}]
      (dat/transact! state/conn
                     [{:topic/id (dat/uuid)
                       :topic/group [:group/id group-id]
                       :topic/user [:user/id user-id]
                       :topic/title title
                       :topic/description description}]))}

   {:id :api/remove-topic!
    :params {:topic-id uuid?
             :user-id uuid?
             :group-id uuid?}
    :conditions
    (fn [{:keys [user-id group-id topic-id]}]
      [(entity-exists?-condition :user/id user-id)
       (entity-exists?-condition :topic/id topic-id)
       (entity-exists?-condition :group/id group-id)
       (topic-is-within-group?-condition topic-id group-id)
       (user-is-admin-of-group?-condition user-id group-id)])
    :effect
    (fn [{:keys [topic-id]}]
      ;; TODO
      )}])

(tada/register! (concat queries commands))

