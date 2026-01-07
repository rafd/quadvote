(ns ^:figwheel-hooks
  quadvote.core
  (:require
   [bloom.commons.pages :as pages]
   [bloom.omni.reagent :as r]
   [quadvote.ui :refer [app-view]]))

(enable-console-print!)

(defn render
  []
  (r/render [app-view]))

(defn ^:export init
  []
  (pages/initialize! [])
  (render))

(defn ^:after-load reload
  []
  (render))
