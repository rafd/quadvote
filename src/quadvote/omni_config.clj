(ns quadvote.omni-config
  (:require
   [quadvote.config :as config]
   [quadvote.routes :as routes]))

(def omni-config
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
    :tailwind-opts {:base-css-rules '[girouette.tw.preflight/preflight-v2_0_3]}}})
