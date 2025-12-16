(ns quadvote.core
  (:require
   [bloom.omni.core :as omni]
   [quadvote.cqrs] ;; to register tada events
   [quadvote.routes :as routes]
   [quadvote.config :refer [config]]))

(defn start! []
  (omni/start!
    omni/system
    {:omni/http-port (:http-port config)
     :omni/title "quadvote"
     :omni/environment :dev
     :omni/api-routes #'routes/routes
     :omni/cljs {:main "quadvote.core"}
     :omni/auth {:cookie {:name "quadvote"
                          :secret (:omni-secret config)
                          :same-site :strict}}
     :omni/css
     {:tailwind? true
      :tailwind-opts {:base-css-rules '[girouette.tw.preflight/preflight-v2_0_3]}}}))

(defn stop! []
  (omni/stop!))
