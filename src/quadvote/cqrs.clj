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

(def commands
  [{:id :api/create-topic!
    :params {:id uuid?
             :text string?
             :user-id uuid?}
    :conditions
    (fn [{:keys [id text user-id]}]
      [[#(state/user-with-id-exists? user-id)
        :invalid "User with this id does not exist."]
       [#(not (state/topic-with-id-exists? id))
        :invalid "Topic with this id already exists."]])
    :effect
    (fn [{:keys [id text user-id]}]
      (state/create-topic! id text))}

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
      [[#(state/user-with-id-exists? user-id)
        :invalid "User with this id does not exist."]
       [#(state/topic-with-id-exists? topic-id)
        :invalid "Topic with this id does not exist."]
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
      [[#(state/user-with-id-exists? user-id)
        :invalid "User with this id does not exist."]])
    ;; secret exists
    ;; user hasn't claimed this secret already
    :effect
    (fn [{:keys [user-id]}]
      (state/claim-token! user-id))}

   ;; admin-only

   #_{:id :api/remove-topic!
      :conditions
      (fn [{:keys [user-id]}]
        [[#(state/user-with-id-exists? user-id)
          :invalid "User with this id does not exist."]])
      ;; is admin
      ;; topic exists
      }
   ])

(tada/register! (concat queries commands))

