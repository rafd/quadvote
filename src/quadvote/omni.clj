(ns quadvote.omni
  (:require
   [bloom.omni.core :as omni]
   [quadvote.routes :as routes]))

(def component
  {:sys.component/id :http
   :sys.component/expects #{:http-port
                            :environment
                            :omni-secret
                            :routes}
   :sys.component/start
   (fn [{:keys [http-port environment routes omni-secret]}]
     (omni/start! {:omni/http-port http-port
                   :omni/title "quadvote"
                   :omni/environment environment
                   :omni/api-routes routes
                   :omni/cljs {:main "quadvote.core"}
                   :omni/auth {:cookie {:name "quadvote"
                                        :secret omni-secret
                                        :same-site :strict}
                               :token {:secret omni-secret}}
                   :omni/html-head-includes
                   [[:link
                     {:rel "stylesheet"
                      :href "/css/markdown.css"}]]
                   :omni/css {:tailwind? true}})
     {})
   :sys.component/stop (fn [_]
                         (omni/stop!))})
