(ns quadvote.jobs
  (:require
   [chime.core :as chime]
   [dat.api :as dat]
   [quadvote.state :as state])
  (:import
   (java.time DayOfWeek Period ZonedDateTime ZoneId LocalTime)))

(defn grant-day?
  [frequency ^ZonedDateTime zdt]
  (case frequency
    :grant-frequency/daily true
    :grant-frequency/weekly (= DayOfWeek/MONDAY (.getDayOfWeek zdt))
    :grant-frequency/monthly (= 1 (.getDayOfMonth zdt))
    false))

(defn to-grant
  [zdt]
  (->> (dat/q '[:find ?group-id ?frequency ?amount
                :where
                [?g :group/id ?group-id]
                [?g :group/grant-frequency ?frequency]
                [?g :group/grant-amount ?amount]]
              @(state/conn))
       (filter (fn [[_ frequency _]]
                 (grant-day? frequency zdt)))))

#_(to-grant (ZonedDateTime/parse "2024-01-01T00:00:00-05:00[America/Toronto]"))

(defn grant-to-eligible-groups!
  [zdt]
  (doseq [[group-id _ amount] (to-grant zdt)]
    (state/grant-to-group! group-id amount)))

(defn start-grant-job! []
  (chime/chime-at
   (->> (chime/periodic-seq
         (.adjustInto (LocalTime/of 0 0)
                      (ZonedDateTime/now (ZoneId/of "America/Toronto")))
         (Period/ofDays 1)))
   (fn [zdt]
     (println "Running grant job...")
     (grant-to-eligible-groups! zdt))))

#_(start-grant-job!)

(def component
  {:sys.component/id :jobs
   :sys.component/expects #{:conn}
   :sys.component/provides #{:grant-job}
   :sys.component/start (fn [_]
                          {:grant-job (start-grant-job!)})
   :sys.component/stop (fn [{:keys [grant-job]}]
                         (.close grant-job))})
