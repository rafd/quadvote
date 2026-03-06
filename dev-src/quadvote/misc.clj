(ns quadvote.misc
  (:require
   [clojure.string]
   [clojure.data.csv]
   [clojure.java.io]
   [dat.api]
   [tada.events.core]
   [quadvote.cqrs]
   [quadvote.routes]
   [quadvote.state]))

(defn set-all-last-visit-at-yesterday!
  []
  (let [yesterday (-> (java.time.Instant/now)
                      (.atZone (java.time.ZoneId/systemDefault))
                      (.minusDays 1)
                      (.toInstant)
                      (java.util.Date/from))
        membership-ids (dat.api/q '[:find [?membership-id ...]
                                    :where [_ :membership/id ?membership-id]]
                                  @(quadvote.state/conn))]
    (dat.api/transact!
     (quadvote.state/conn)
     (->> membership-ids
          (map (fn [membership-id]
                 {:membership/id membership-id
                  :membership/last-visit-at yesterday}))))))

(comment

  ;; transact latest schema
  (datascript.core/reset-schema!
   (@quadvote.state/conn :dat.api/conn)
   (dat.api/->db-schema :dat.db/datascript
                        quadvote.state/schema))

  ;; create admin user
  (quadvote.state/create-user!
   {:name "Raf" :email "rafal.dittwald@gmail.com" :admin? true})

  (def admin-id (quadvote.state/email->user-id "admin@example.com"))

  ;; create users
  (doseq [[name email] (->> (slurp "/tmp/people.csv")
                            (clojure.string/split-lines)
                            (map #(clojure.string/split % #",")))]
    (tada.events.core/do!
     :api/create-user!
     {:user-id admin-id
      :name name
      :email email}))

  ;; grant bonus tokens
  (quadvote.state/grant-to-group!
   (dat.api/q '[:find ?group-id .
                :where [_ :group/id ?group-id]]
              @quadvote.state/conn)
   25)

  ;; create topics
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
    :user-id (quadvote.state/email->user-id "")})

  ;; check state
  (clojure.pprint/pprint (deref quadvote.state/state)))
