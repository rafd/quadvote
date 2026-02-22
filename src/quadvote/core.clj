(ns quadvote.core
  (:gen-class)
  (:require
   [sys.api :as sys]
   ;; components
   [quadvote.omni :as omni]
   [quadvote.config :as config]
   [quadvote.jobs :as jobs]
   [quadvote.routes :as routes]
   [quadvote.cqrs :as cqrs]
   [quadvote.state :as state]))

(sys/set! :system
          [config/component
           state/component
           omni/component
           routes/component
           cqrs/component
           jobs/component])

(defn start! []
  (sys/start! :system))

(defn stop! []
  (sys/stop! :system))

(defn -main
  [& _]
  (start!))

#_(start!)

