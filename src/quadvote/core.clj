(ns quadvote.core
  (:require
   [bloom.omni.core :as omni]
   [quadvote.cqrs] ;; to register tada events
   [quadvote.routes :as routes]
   [quadvote.config :as config]))

(defn start! []
  (omni/start!
    omni/system
    {:omni/http-port (config/get :http-port)
     :omni/title "quadvote"
     :omni/environment :dev
     :omni/api-routes #'routes/routes
     :omni/cljs {:main "quadvote.core"}
     :omni/auth {:cookie {:name "quadvote"
                          :secret (config/get :omni-secret)
                          :same-site :strict}
                 :token {:secret (config/get :omni-secret)}}
     :omni/css
     {:tailwind? true
      :tailwind-opts {:base-css-rules '[girouette.tw.preflight/preflight-v2_0_3]}}}))

(defn stop! []
  (omni/stop!))
