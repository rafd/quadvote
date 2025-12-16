(ns quadvote.config
  (:require
   [bloom.commons.config :as config]))

(def config
  (config/read
    "config.edn"
    [:map
     [:http-port integer?]
     [:omni-secret string?]
     [:environment [:enum :prod :dev]]]))
