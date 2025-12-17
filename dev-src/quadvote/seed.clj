(ns quadvote.seed
  (:require
    [bloom.commons.uuid :as uuid]
    [tada.events.core :as tada]
    [quadvote.state :as state]
    [quadvote.model :as model]))

(defn generate-user []
  (let [uuid (uuid/random)
        name (apply str (take 5 (re-seq #"[a-z]" (str uuid))))]
    {:id uuid
     :name name
     :email (str name "@example.com")}))

(defn generate-topic []
  {:text (str "Foo bar baz " (rand-int 10000))})

(defn seed! []
  (reset! state/state state/initial-state)

  (state/create-user!
   {:name "Admin"
    :email "admin@example.com"
    :admin? true})

  (let [admin-user-id (-> @state/state
                          :db/users
                          keys
                          first)]

    (dotimes [_ 10]
      (tada/do! :api/create-user!
                (assoc (generate-user)
                       :user-id admin-user-id)))

    (dotimes [_ 10]
      (tada/do! :api/create-topic!
                (assoc (generate-topic)
                        :user-id admin-user-id)))

    (let [users (-> @state/state
                    :db/users
                    vals)
          topics (-> @state/state
                     :db/topics
                     vals)]
      (doall
       (for [topic topics
             user users
             :when (rand-nth [true false])]
         (do
           (swap! state/state assoc-in [:db/balances (:user/id user)] 25)
           (tada/do! :api/vote!
                     {:vote-id (uuid/random)
                      :topic-id (:topic/id topic)
                      :voice-amount (inc (rand-int model/max-voice-amount-per-vote))
                      :user-id (:user/id user)})))))))

#_(seed!)

#_(:db/users @state/state)
