(ns quadvote.seed
  (:require
    [bloom.commons.uuid :as uuid]
    [quadvote.state :as state]))

(defn generate-user []
  {:user/id (uuid/random)
   :user/name "alice"
   :user/email "alice@example.com"})

(defn generate-topic []
  {:topic/id (uuid/random)
   :topic/text (str "Foo bar baz " (rand-int 10000))})

(defn generate-state []
  (let [topics (repeatedly 10 generate-topic)
        users (repeatedly 10 generate-user)
        votes (->> (for [topic topics
                         user users]
                     (when (rand-nth [true false])
                       {:vote/id (uuid/random)
                        :vote/user-id (:user/id user)
                        :vote/topic-id (:topic/id topic)
                        :vote/voice-amount (inc (rand-int state/max-voice-amount-per-vote))}))
                   (remove nil?))]
    {:db/topics (zipmap (map :topic/id topics)
                        topics)
     :db/users (zipmap (map :user/id users)
                       users)
     :db/balances (zipmap (map :user/id users)
                          (map (fn [_] (rand-int 100))
                               users))
     :db/votes (zipmap (map :vote/id votes)
                       votes)}))

#_(reset! state/state (generate-state))

#_(:db/users @state/state)
