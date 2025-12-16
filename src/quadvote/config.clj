(ns quadvote.config
  (:require
   [bloom.commons.config :as config]))

(def config
  (config/read
    "config.edn"
    [:map
     [:http-port :int]
     [:omni-secret :string]
     [:environment [:enum :prod :dev]]
     [:db-file-path :string]]))

