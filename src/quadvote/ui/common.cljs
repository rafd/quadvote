(ns quadvote.ui.common
  (:require
   [reagent.core :as r]))

(defn gem-icon []
  [:span "⧫"])

(defn popover
  [{:keys [position]} popover-content popover-trigger]
  (r/with-let
   [container-el (atom nil)
    open? (r/atom false)]
   [:div {:ref (fn [el]
                 (when el
                   (reset! container-el el)))
          :on-mouse-over (fn []
                           (reset! open? true))

          :on-mouse-out (fn []
                          (reset! open? false))}
    [:div {}
     popover-trigger]
    (when @open?
      [:div {:style (merge
                     {:position "absolute"
                      :z-index 1000}
                     (when @container-el
                       (let [rect (.getBoundingClientRect @container-el)]
                         (case position
                           :right
                           {:top (str (.-top rect) "px")
                            :left (str (+ (.-left rect) (.-width rect)) "px")}
                           :left
                           {:top 0
                            :right (str (.-offsetWidth @container-el) "px")}
                           {}))))
             :tw "bg-white shadow"}
       popover-content])]))
