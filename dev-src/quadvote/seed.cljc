(ns quadvote.seed
  (:require
    [bloom.commons.uuid :as uuid]
    [quadvote.model :as model]))

(defn generate-user []
  (let [uuid (uuid/random)
        name (apply str (take 5 (re-seq #"[a-z]" (str uuid))))]
    {:user/id uuid
     :user/name name
     :user/email (str name "@example.com")}))

(defn generate-topic []
  {:topic/id (uuid/random)
   :topic/text (str "Foo bar baz " (rand-int 10000))})

(defn generate-state []
  (let [topics (repeatedly 10 generate-topic)
        users (conj (repeatedly 10 generate-user)
                    {:user/id (uuid/random)
                     :user/name "Admin"
                     :user/admin? true
                     :user/email "admin@example.com"})
        votes (->> (for [topic topics
                         user users]
                     (when (rand-nth [true false])
                       {:vote/id (uuid/random)
                        :vote/user-id (:user/id user)
                        :vote/topic-id (:topic/id topic)
                        :vote/voice-amount (inc (rand-int model/max-voice-amount-per-vote))}))
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

#_(reset! quadvote.state/state (generate-state))

#_(:db/users @state/state)
