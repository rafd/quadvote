(ns quadvote.ui.modal
  (:require
   [reagent.core :as r]))

(defonce state (r/atom nil))

(defn open!
  [view]
  (reset! state view))

(defn close!
  []
  (reset! state nil))

(defn modal-view []
  (when-let [view @state]
    [:div.wrapper {:tw "fixed top-0 right-0 left-0 bottom-0 flex p-10 cursor-pointer items-center justify-center z-1000"
                   :style {:background-color "rgba(0,0,0,0.3)"}
                   :on-click (fn []
                               (close!))}
     [:div.modal {:tw "shadow bg-white rounded w-full max-h-full max-w-40rem p-5 cursor-default overflow-y-auto"
                  :on-click-capture (fn [e]
                                      (.stopPropagation e))}
      view]]))
