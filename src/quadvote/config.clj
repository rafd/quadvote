(ns quadvote.config
  (:refer-clojure :exclude [get])
  (:require
   [malli.core :as m]
   [sys.api :as sys]
   [bloom.commons.config :as config]))

(def schema
  [:map
   [:http-port :int]
   [:omni-secret :string]
   [:environment [:enum :prod :dev]]
   [:website-base-url :string]
   [:db-path :string]
   [:smtp-credentials
    [:maybe
     [:map
      [:port :int]
      [:host :string]
      [:tls :boolean]
      [:from :string]
      [:user :string]
      [:pass :string]]]]])

(def component
  {:sys.component/id :config
   :sys.component/provides (->> (malli.core/children schema)
                                (map first)
                                set)
   :sys.component/start (fn [_]
                          (config/read "config.edn" schema))})


(defn get
  [k]
  (sys/get :system k))
