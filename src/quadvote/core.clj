(ns quadvote.core
  (:gen-class)
  (:require
   [bloom.omni.core :as omni]
   [quadvote.cqrs] ;; to register tada events
   [quadvote.jobs :as jobs]
   [quadvote.omni-config :refer [omni-config]]))

(defn start! []
  (omni/start! omni/system omni-config)
  (jobs/schedule-grant-job!))

(defn stop! []
  (omni/stop!))

(defn -main
  [& _]
  (start!))
