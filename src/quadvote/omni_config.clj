(ns quadvote.omni-config)

;; this is only to satisfy omni during uberjaring
(def omni-config
  {:omni/environment :prod
   :omni/cljs {:main "quadvote.core"}
   :omni/css {:tailwind? true}})
