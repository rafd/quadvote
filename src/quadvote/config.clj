(ns quadvote.config
  (:refer-clojure :exclude [get])
  (:require
   [bloom.commons.config :as config]))

(def config
  (delay
    (config/read
     "config.edn"
     [:map
      [:http-port :int]
      [:omni-secret :string]
      [:environment [:enum :prod :dev]]
      [:website-base-url :string]
      [:db-file-path :string]
      [:smtp-credentials
       {:optional true}
       [:map
        [:port :int]
        [:host :string]
        [:tls :boolean]
        [:from :string]
        [:user :string]
        [:pass :string]]]])))

(defn get [key]
  (clojure.core/get @config key))
