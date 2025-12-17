(ns quadvote.cqrs
  (:require
    [tada.events.core :as tada]
    [quadvote.state :as state]))

(def queries
  [{:id :api/data
    :params {:user-id uuid?}
    :return
    (fn [{:keys [user-id]}]
      {:api.data/topics (vals (:db/topics @state/state))
       :api.data/topic-voice-amounts (state/votes->topic-voice-amounts (vals (:db/votes @state/state)))
       :api.data/user (get-in @state/state [:db/users user-id])
       :api.data/balance (get-in @state/state [:db/balances user-id] 0)
       :api.data/votes (->> (:db/votes @state/state)
                            vals
                            (filter (fn [vote]
                                      (= user-id (:vote/user-id vote)))))})}])

(defn user-with-id-exists?-condition
  [user-id]
  [#(state/user-with-id-exists? user-id)
   :invalid "User with this id does not exist."])

(defn user-is-admin?-condition
  [user-id]
  [#(state/user-is-admin? user-id)
   :forbidden "User is not an admin."])

(defn topic-with-id-exists?-condition
  [topic-id]
  [#(state/topic-with-id-exists? topic-id)
   :invalid "Topic with this id does not exist."])

(def commands
  [

   {:id :api/vote!
    :params {:vote-id uuid?
             :topic-id uuid?
             :voice-amount (fn [x]
                             (and
                               (int? x)
                               (<= 0 x 5)))
             :user-id uuid?}
    :conditions
    (fn [{:keys [vote-id topic-id voice-amount user-id]}]
      [(user-with-id-exists?-condition user-id)
       (topic-with-id-exists?-condition topic-id)
       [#(state/user-has-no-other-vote-for-topic? user-id vote-id topic-id)
        :invalid "User has another vote for this topic"]
       [#(state/user-can-afford? {:user-id user-id
                                  :topic-id topic-id
                                  :vote-id vote-id
                                  :voice-amount voice-amount})
        :invalid "User has insufficient tokens to upvote"]])
    :effect
    (fn [{:keys [vote-id topic-id voice-amount user-id]}]
      (state/vote! vote-id topic-id user-id voice-amount))}

   {:id :api/claim-tokens!
    :params {:secret string?
             :user-id uuid?}
    :conditions
    (fn [{:keys [secret user-id]}]
      [(user-with-id-exists?-condition user-id)])
    ;; secret exists
    ;; user hasn't claimed this secret already
    :effect
    (fn [{:keys [user-id]}]
      (state/claim-token! user-id))}

   ;; admin-only

   {:id :api/create-user!
    :params {:name string?
             :email string?
             :user-id uuid?}
    :conditions
    (fn [{:keys [user-id email]}]
      [(user-with-id-exists?-condition user-id)
       (user-is-admin?-condition user-id)
       [#(nil? (state/user-by-email email))
        :invalid "User with this email already exists."]])
    :effect
    (fn [{:keys [name email]}]
      (state/create-user! {:name name :email email}))}

   {:id :api/create-topic!
    :params {:text string?
             :user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [(user-with-id-exists?-condition user-id)
       (user-is-admin?-condition user-id)])
    :effect
    (fn [{:keys [text]}]
      (state/create-topic! text))}

   {:id :api/remove-topic!
    :params {:topic-id uuid?
             :user-id uuid?}
    :conditions
    (fn [{:keys [user-id topic-id]}]
      [(user-with-id-exists?-condition user-id)
       (topic-with-id-exists?-condition topic-id)
       (user-is-admin?-condition user-id)])
    :effect
    (fn [{:keys [topic-id]}]
      #_(state/remove-topic! topic-id))}])

(tada/register! (concat queries commands))

