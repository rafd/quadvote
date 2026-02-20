(ns quadvote.seed
  (:require
    [bloom.commons.uuid :as uuid]
    [tada.events.core :as tada]
    [quadvote.state :as state]
    [quadvote.model :as model]
    [dat.api :as dat]))

(defonce uuid
  (memoize (fn [x]
             (uuid/random))))

(defn generate-user [i]
  (let [id (uuid i)
        name (apply str (take 10 (re-seq #"[a-z]" (str id))))]
    {:name name
     :email (str name "@example.com")}))

(defn generate-topic []
  {:title (str "Foo bar baz " (rand-int 10000))
   :description "Lorep ipsum...\n\n Read more [here](https://example.com)."})

(defn seed! []
  (dat/clear! state/conn)

  (dat/transact!
   state/conn
   [{:user/id (uuid ::admin)
     :user/name "Admin"
     :user/email "admin@example.com"}])

  (let [admin-user-id (dat/q '[:find ?id .
                               :where
                               [_ :user/id ?id]]
                             @state/conn)]

    (tada/do! :api/create-group!
              (assoc {:name "Test Group A"}
                     :user-id admin-user-id))

    (tada/do! :api/create-group!
              (assoc {:name "Test Group B"}
                     :user-id admin-user-id))

    (doseq [group-id (dat/q '[:find [?id ...]
                              :where
                              [_ :group/id ?id]]
                            @state/conn)]

      (dotimes [i 10]
        (tada/do! :api/add-user-to-group!
                  (assoc (generate-user (str "user-" i))
                         :user-id admin-user-id
                         :group-id group-id)))

      (dotimes [_ 10]
        (tada/do! :api/create-topic!
                  (assoc (generate-topic)
                         :group-id group-id
                         :user-id admin-user-id)))

      (let [users (dat/q '[:find [(pull ?e [*]) ...]
                           :where
                           [?e :user/id _]]
                         @state/conn)
            topics (dat/q '[:find [(pull ?e [*]) ...]
                            :where
                            [?e :topic/id _]]
                          @state/conn)]
        (doall
         (for [topic topics
               user users
               :when (rand-nth [true false])
               :let [membership-id (dat/q '[:find ?id .
                                            :in $ ?user-id ?group-id
                                            :where
                                            [?u :user/id ?user-id]
                                            [?g :group/id ?group-id]
                                            [?m :membership/user ?u]
                                            [?m :membership/group ?g]
                                            [?m :membership/id ?id]]
                                          @state/conn
                                          (:user/id user)
                                          group-id)]]
           (do
             (tada/do! :api/claim!
                       {:membership-id membership-id
                        :user-id (:user/id user)})

             (try
               (tada/do! :api/vote!
                         {:topic-id (:topic/id topic)
                          :voice-amount (inc (rand-int model/max-voice-amount-per-vote))
                          :user-id (:user/id user)})
               (catch Exception _
                 nil)))))

        (for [topic topics
              :when (rand-nth [false false false true])]
          (tada/do! :api/burn-topic!
                    {:topic-id (:topic/id topic)
                     :message "asdf"
                     :user-id admin-user-id}))))))

#_(seed!)

