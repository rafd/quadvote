(ns quadvote.seed
  (:require
    [bloom.commons.uuid :as uuid]
    [dat.api :as dat]
    [tada.events.core :as tada]
    [sys.api :as sys]
    [quadvote.cqrs :as cqrs]
    [quadvote.state :as state]
    [quadvote.model :as model]))

(defn uuid [x]
  (uuid/from-email (str x)))

(defn generate-user [i]
  (let [id (uuid i)
        name (apply str (take 10 (re-seq #"[a-z]" (str id))))]
    {:name name
     :email (str name "@example.com")}))

(defn generate-topic []
  {:title (str "Foo bar baz " (rand-int 10000))
   :description "Lorep ipsum...\n\n Read more [here](https://example.com)."})

;; only use public interfaces to seed (ie. tada/do!)
;; except when absolutely necessary

(defn tada-do! [event-id args]
  (tada/do! (cqrs/registry) event-id args))

(defn seed! []
  (dat/clear! (state/conn))

  (dat/transact!
   (state/conn)
   [{:user/id (uuid ::admin)
     :user/name "Admin"
     :user/email "admin@example.com"}])

  (let [admin-user-id (dat/q '[:find ?id .
                               :where
                               [_ :user/id ?id]]
                             @(state/conn))]

    (tada-do! :api/create-group!
              {:name "Test Group A"
               :user-id admin-user-id})

    (tada-do! :api/create-group!
              {:name "Test Group B"
               :user-id admin-user-id})

    (doseq [[group-id
             {:keys [open-membership? open-topics? grant-frequency grant-amount description]}]
            (->> (dat/q '[:find [?id ...]
                          :where
                          [_ :group/id ?id]]
                        @(state/conn))
                 (map-indexed vector)
                 (map (fn [[index group-id]]
                        [group-id
                         (get [{:open-membership? false
                                :open-topics? false
                                :description "Lorem ipsum..."
                                :grant-frequency :grant-frequency/monthly
                                :grant-amount 25}
                               {:open-membership? true
                                :open-topics? true
                                :description "Dolor sit amet..."
                                :grant-frequency :grant-frequency/weekly
                                :grant-amount 10}] index)])))]

      (tada-do! :api/update-group!
                {:group-id group-id
                 :user-id admin-user-id
                 :open-membership? open-membership?
                 :open-topics? open-topics?
                 :description description
                 :grant-frequency grant-frequency
                 :grant-amount grant-amount})

      (dotimes [i 10]
        (tada-do! :api/add-user-to-group!
                  (assoc (generate-user (str "user-" i))
                         :user-id admin-user-id
                         :group-id group-id)))

      (dotimes [_ 10]
        (tada-do! :api/create-topic!
                  (assoc (generate-topic)
                         :group-id group-id
                         :user-id admin-user-id)))

      (let [users (dat/q '[:find [(pull ?e [*]) ...]
                           :where
                           [?e :user/id _]]
                         @(state/conn))
            topics (dat/q '[:find [(pull ?e [*]) ...]
                            :where
                            [?e :topic/id _]]
                          @(state/conn))]
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
                                          @(state/conn)
                                          (:user/id user)
                                          group-id)]]
           (do
             (tada-do! :api/claim!
                       {:membership-id membership-id
                        :user-id (:user/id user)})

             (try
               (tada-do! :api/vote!
                         {:topic-id (:topic/id topic)
                          :voice-amount (inc (rand-int model/max-voice-amount-per-vote))
                          :user-id (:user/id user)})
               (catch Exception _
                 nil)))))

        (for [topic topics
              :when (rand-nth [false false false true])]
          (tada-do! :api/burn-topic!
                    {:topic-id (:topic/id topic)
                     :message "asdf"
                     :user-id admin-user-id}))))))

#_(seed!)

