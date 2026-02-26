(ns quadvote.ui.gem-animation
  (:require
   [reagent.core :as r]))

(defonce *animations (r/atom []))

(defn- gem-view
  [{:keys [from to delay]}]
  (let [*el (r/atom nil)
        arc-x (* (- (rand) 0.5) 50)
        arc-y (* (- (rand) 0.5) 50)]
    (r/create-class
     {:display-name "gem"
      :component-did-mount
      (fn [_]
        (when-let [el @*el]
          (.animate
           el
           (clj->js
            [{:transform (str "translate(" (:x from) "px," (:y from) "px) rotate(0deg) scale(1)")
              :opacity 1}
             {:transform (str "translate("
                              (+ (/ (+ (:x from) (:x to)) 2) arc-x) "px,"
                              (+ (/ (+ (:y from) (:y to)) 2) arc-y) "px) rotate(180deg) scale(1.2)")
              :opacity 1
              :offset 0.5}
             {:transform (str "translate(" (:x to) "px," (:y to) "px) rotate(360deg) scale(0.4)")
              :opacity 0}])
           (clj->js
            {:duration 550
             :delay delay
             :easing "ease-in-out"
             :fill "both"}))))
      :reagent-render
      (fn [_]
        [:div {:ref (fn [el] (reset! *el el))
               :style {:position "fixed"
                       :left 0
                       :top 0
                       :pointer-events "none"
                       :z-index 9999
                       :transform (str "translate(" (:x from) "px," (:y from) "px)")
                       :color "#7c3aed"
                       :font-size "20px"
                       :line-height 1
                       :margin-left "-7px"
                       :margin-top "-7px"}}
         "⧫"])})))

(defn overlay-view []
  (into [:<>]
        (for [{:keys [id from to count]} @*animations
              i (range count)]
          ^{:key (str id "-" i)}
          [gem-view {:from from :to to :delay (* i 50)}])))

(defn trigger!
  [{:keys [from-el to-el gem-count]}]
  (when (and from-el to-el (pos? gem-count))
    (let [from-rect (.getBoundingClientRect from-el)
          to-rect (.getBoundingClientRect to-el)
          from {:x (+ (.-left from-rect) (/ (.-width from-rect) 2))
                :y (+ (.-top from-rect) (/ (.-height from-rect) 2))}
          to {:x (+ (.-left to-rect) (/ (.-width to-rect) 2))
              :y (+ (.-top to-rect) (/ (.-height to-rect) 2))}
          id (str (random-uuid))
          total-ms (+ (* (dec gem-count) 50) 700)]
      (swap! *animations conj {:id id :from from :to to :count gem-count})
      (js/setTimeout
       (fn []
         (swap! *animations (fn [anims]
                              (vec (remove (fn [a] (= (:id a) id)) anims)))))
       total-ms))))
