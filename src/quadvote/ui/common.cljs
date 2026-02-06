(ns quadvote.ui.common
  (:require
   [reagent.core :as r]))

(def purple "#7c3aed")

(def input-tw "p-1 border rounded")

(defn color [x]
  (str "oklch(70% 20%" (hash x) ")"))

(defn token-icon []
  [:span "⧫"])

(defn token-amount-view [amount ?prefix]
  [:div {:tw "flex gap-0.5 items-center rounded px-1 justify-end"
         :style {:background-color "#e2ddf3"
                 :border (str "1px solid " purple)
                 :color purple}}
   [:span {:style {:font-family "monospace"}}
    (case ?prefix
      :refund "⤴"
      :cost [:div {:style {:transform "rotate(90deg)"}} "⤵"]
      :current "="
      :gain "+"
      nil)]
   [:span (Math/abs amount)] [token-icon]])

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
                       (let [rect (.getBoundingClientRect @container-el)
                             scroll-x (.-scrollX js/window)
                             scroll-y (.-scrollY js/window)]
                         (case position
                           :right
                           {:top (str (+ (.-top rect) scroll-y) "px")
                            :left (str (+ (.-left rect) (.-width rect) scroll-x) "px")}
                           :left
                           {:top (str (+ (.-top rect) scroll-y) "px")
                            :right (str (+ (.-right rect) scroll-x) "px")}
                           {}))))}
       popover-content])]))

(defn button
  [opts & children]
  (into [:button (merge
                  {:tw "flex gap-1 items-center border border-gray-300 p-1 rounded"}
                  opts)]
        children))

(defn user-circles
  [users]
  [:div.users {:tw "flex -space-x-1.5"}
   (for [{:user/keys [id name]} users]
     ^{:key id}
     [:div {:tw "rounded-full text-white text-center text-xs w-5 h-5 leading-5 border border-white"
            :title name
            :style {:background (color id)}}
      (first name)])])


