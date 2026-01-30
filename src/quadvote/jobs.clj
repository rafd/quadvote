(ns quadvote.jobs
  (:require
   [chime.core :as chime]
   [dat.api :as dat]
   [quadvote.state :as state])
  (:import
   (java.time Period ZonedDateTime ZoneId LocalTime)))

(defn grant-to-all-groups!
  []
  (doseq [group-id (dat/q '[:find [?group-id]
                            :where [_ :group/id ?group-id]]
                          @state/conn)]
    (state/grant-to-group! group-id 25)))

#_(grant-to-all-groups!)

(defn schedule-grant-job! []
  (chime/chime-at
   (first (->> (chime/periodic-seq
                (.adjustInto (LocalTime/of 0 0)
                             (ZonedDateTime/now (ZoneId/of "America/Toronto")))
                (Period/ofDays 1))
               (filter (fn [instant]
                         (= 1 (.getDayOfMonth instant))))))
   (fn [_]
     (println "Running job...")
     (grant-to-all-groups!))))

#_(schedule-grant-job!)

