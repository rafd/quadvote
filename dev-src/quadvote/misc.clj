(ns quadvote.misc
  (:require
   [clojure.string]
   [clojure.data.csv]
   [clojure.java.io]
   [tada.events.core]
   [quadvote.cqrs]
   [quadvote.routes]
   [quadvote.state]))

(comment
  ;; create admin user
  (quadvote.state/create-user!
   {:name "Raf" :email "rafal.dittwald@gmail.com" :admin? true})

  (def admin-id (:user/id (quadvote.state/user-by-email "rafal.dittwald@gmail.com")))
  ;; create users

  (doseq [[name email] (->> (slurp "/tmp/people.csv")
                            (clojure.string/split-lines)
                            (map #(clojure.string/split % #",")))]
    (tada.events.core/do!
     :api/create-user!
     {:user-id admin-id
      :name name
      :email email}))

  ;; grant tokens
  (let [amount 25]
    (doseq [user-id (->> @quadvote.state/state
                         :db/users
                         keys)]
      (swap! quadvote.state/state update-in [:db/balances user-id]
             (fnil + 0) amount)))

  (doseq [[title description] (->> (with-open [reader (clojure.java.io/reader "projects.csv")]
                                     (doall
                                      (clojure.data.csv/read-csv reader)))

                                   rest)]
    (tada.events.core/do!
     :api/create-topic!
     {:user-id admin-id
      :title title
      :description description}))

  ;; login link
  (quadvote.routes/wrap-login
   {:url "/"
    :user-id (quadvote.state/user-by-email "")})

  ;; check state
  (clojure.pprint/pprint (deref quadvote.state/state)))
