(ns quadvote.ui.modal
  (:require
   [goog.object :as o]
   [reagent.core :as r]))

(defonce state (r/atom nil))

(defn open!
  [modal-id]
  (reset! state modal-id))

(defn close!
  []
  (reset! state nil))

(defn new-topic-modal-view []
  [:form {:on-submit (fn [e]
                       (.preventDefault e)
                       (js/console.log (-> e .-target .-elements (o/get "text") .-value)))}
   [:h1 "Suggest a Topic"]
   [:textarea {:name "text"
               :required true
               :auto-focus true}]
   [:button "Submit"]])

(defn info-modal-view []
  [:div {:tw "prose"}
   [:p "Vote on which projects Raf should spend more time on."]
   [:p ]
   [:p "You have tokens to spend across projects."]
   [:p "You can vote multiple times per project, but concentrating votes is more expensive:"]
  [:table
    [:tr [:th "Tokens =>"] [:th "Votes"]]
    [:tr [:td "1"] [:td "1"]]
    [:tr [:td "4"] [:td "2"]]
    [:tr [:td "9"] [:td "3"]]
    [:tr [:td "16"] [:td "4"]]
    [:tr [:td "25"] [:td "5"]]]
   [:p "You can adjust your votes any time."]
   [:p "When Raf spends a day on a project, tokens spent on that project will be consumed."]
   [:p "New tokens can be claimed by logging in once a month."]
   [:p "(This is an experiment in applied " [:a {:tw "underline decoration-from-font"
                                                 :target "_blank"
                                                 :href "https://en.wikipedia.org/wiki/Quadratic_voting"} "quadratic voting"] ")"]])

(defn modal-view []
  (when-let [modal-id @state]
    [:div.wrapper {:tw "absolute top-0 right-0 left-0 bottom-0 flex p-10 cursor-pointer items-center justify-center"
                   :style {:background-color "rgba(0,0,0,0.3)"}
                   :on-click (fn []
                               (close!))}
     [:div.modal {:tw "shadow bg-white rounded w-full max-h-full max-w-40rem p-10 cursor-default overflow-y-auto"
                  :on-click-capture (fn [e]
                                      (.stopPropagation e))}
      (case modal-id
        :modal/new-topic
        [new-topic-modal-view]
        :modal/info
        [info-modal-view])]]))
