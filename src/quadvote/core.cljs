(ns ^:figwheel-hooks
  quadvote.core
  (:require
   [bloom.omni.reagent :as r]
   [re-frame.core :refer [dispatch-sync]]
   [quadvote.ui.state] ;; to load reframe events
   [quadvote.ui :refer [app-view]]))

(enable-console-print!)

(defn render
  []
  (r/render [app-view]))

(defn ^:export init
  []
  (dispatch-sync [:state/initialize!])
  (render))

(defn ^:after-load reload
  []
  (render))
