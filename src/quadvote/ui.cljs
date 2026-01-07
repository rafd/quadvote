(ns quadvote.ui
  (:require
    [bloom.commons.fontawesome :as fa]
    [goog.object :as o]
    [reagent.core :as r]
    [re-frame.core :refer [dispatch subscribe]]
    [markdown.core :as md]
    [quadvote.ui.common :as ui]
    [quadvote.model :as model]
    [quadvote.ui.state :as state]))

(defn new-topic-modal-view []
  [:form {:on-submit (fn [e]
                       (.preventDefault e)
                       (dispatch [:state/create-topic!
                                  (-> e .-target .-elements (o/get "text") .-value)]))}
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
  (when-let [modal-id @(subscribe [:state/modal])]
    [:div.wrapper {:tw "absolute top-0 right-0 left-0 bottom-0 flex p-10 cursor-pointer items-center justify-center"
                   :style {:background-color "rgba(0,0,0,0.3)"}
                   :on-click (fn []
                               (dispatch [:state/open-modal! nil]))}
     [:div.modal {:tw "shadow bg-white rounded w-full max-h-full max-w-40rem p-10 cursor-default overflow-y-auto"
                  :on-click-capture (fn [e]
                                      (.stopPropagation e))}
      (case modal-id
        :modal/new-topic
        [new-topic-modal-view]
        :modal/info
        [info-modal-view])]]))

(defn voting-controls-linear-view [vote]
  [:div.votes {:tw "flex flex-col space-y-3px min-w-3"}
   (for [i (range 0 (:vote/voice-amount vote))]
     ^{:key i}
     [:div.bar {:tw "w-3 h-0.75 bg-green-700 rounded"}])])

(defn topic-view
  [topic]
  (r/with-let
   [show-description? (r/atom false)]
   [:div.topic {:tw "bg-white rounded overflow-hidden shadow"}
    [:div.main {:tw "flex justify-between items-center"}
     [:div.title {:tw "p-4 flex gap-2 items-center cursor-pointer group"
                  :on-click (fn []
                              (swap! show-description? not))}
      (:topic/title topic)
      [fa/fa-info-circle-solid {:tw "w-3 h-3 text-gray-400 group:hover:text-gray-600"}]]

     (let [vote @(subscribe [:state/vote-for-topic (:topic/id topic)])
           can-upvote? (and
                        (< (:vote/voice-amount vote) model/max-voice-amount-per-vote)
                        (model/can-afford?
                         {:balance
                         @(subscribe [:state/my-balance])
                          :old-voice-amount
                          (:vote/voice-amount vote)
                          :new-voice-amount
                          (inc (:vote/voice-amount vote))}))]
       [:div.meta {:tw ["flex px-2 items-center gap-1 self-stretch"
                        (if vote
                          "bg-green-200 border-l border-green-300"
                          "bg-gray-200 border-l border-gray-300")]}
        [:div.total-voice
         ;; set a fixed width, so that width is the same in all rows
         {:tw "w-1.5em text-center"}
         @(subscribe [:state/topic-voice-amount (:topic/id topic)])]

        [:div.diamond
         (let [s 50
               z (/ s (Math/sqrt 2))]
           [:svg {:view-box (str (- z) " " (- (* 2 z)) " " (* 2 z) " " (* 2 z))
                  :width "40px"
                  :height "40px"}
            (for [i (reverse (range 0 model/max-voice-amount-per-vote))]
              ^{:key i}
              [:rect {:x 0
                      :y 0
                      :height (* (/ s model/max-voice-amount-per-vote) (inc i))
                      :width (* (/ s model/max-voice-amount-per-vote) (inc i))
                      :stroke (if (:vote/voice-amount vote)
                                "hsl(141deg 79% 85%)"
                                "hsl(0deg 0% 90%)")
                      :stroke-width "3"
                      :style {:transform "rotate(-135deg)"
                              :fill (cond
                                      (nil? (:vote/voice-amount vote))
                                      "hsl(0deg 0% 90%)"
                                      ;; faded
                                      #_"hsla(143deg 0% 0% / 3%)"
                                      (<= (inc i) (:vote/voice-amount vote))
                                      "hsl(143deg 63% 30%)"
                                      :else
                                      "hsl(141deg 79% 85%)"
                                      ;; faded:
                                      #_"hsla(143deg 63% 30% / 10%)")}}])])]

        [:div.voting {:tw "flex flex-col items-center min-w-6"}
         ;; increase-vote
         (if can-upvote?
           [ui/popover
            {:position :right}
            [:div
             (let [token-count (model/token-cost
                                (:vote/voice-amount vote)
                                (inc (:vote/voice-amount vote)))]
               [:span {:title (str "Increasing your vote by 1 costs you " token-count " tokens")}
                [ui/token-amount-view token-count :cost]])]
            [:button {:tw "px-1 flex gap-1"
                      :on-click (fn [_]
                                  (dispatch [:state/vote!
                                             vote
                                             (:topic/id topic)
                                             (:vote/voice-amount vote)
                                             (inc (:vote/voice-amount vote))]))}
             [fa/fa-caret-up-solid {:tw "w-4 h-4"}]]]
           [:div {:tw "px-1 w-4 h-4"}])

         ;; our voice
         [ui/popover
          {:position :right}
          [:div {:title (str "Your " (:vote/voice-amount vote) " votes, cost "
                             (* (:vote/voice-amount vote) (:vote/voice-amount vote)) " tokens")}
           [ui/token-amount-view (* (:vote/voice-amount vote) (:vote/voice-amount vote)) :current]]
          [:div {:tw "w-4 text-center h-4"}
           (:vote/voice-amount vote)]]

         ;; decrease-vote
         (if vote
           [ui/popover
            {:position :right}
            (let [token-count (model/token-cost
                               (:vote/voice-amount vote)
                               (dec (:vote/voice-amount vote)))]
              [:div {:title (str "Reducing your vote by 1 refunds you " token-count " tokens")}
               [ui/token-amount-view token-count :refund]])
            [:button {:tw "px-1 flex gap-1"
                      :on-click (fn [_]
                                  (dispatch [:state/vote!
                                             vote
                                             (:topic/id topic)
                                             (:vote/voice-amount vote)
                                             (dec (:vote/voice-amount vote))]))}
             [fa/fa-caret-down-solid {:tw "w-4 h-4"}]]]
           [:div {:tw "px-1 w-4 h-4"}])]])]
    (when @show-description?
      [:div.description
       {:tw "px-4 whitespace-pre-wrap prose text-xs border-t border-gray-300"
        :dangerouslySetInnerHTML
        {:__html
         (md/md->html
          (:topic/description topic))}}])]))

(defn header-view []
  [:div.header {:tw "flex justify-between items-center pb-4 gap-3"}
   [:h1 {:tw "grow"} "What should Raf work on?"]
   #_[ui/button {:on-click (fn [] (dispatch [:state/open-modal! :modal/new-topic]))}
      [fa/fa-plus-circle-solid {:tw "w-4 h-4"}]
      "Suggest a Topic"]
   [ui/button {:on-click (fn [] (dispatch [:state/open-modal! :modal/info]))}
    [fa/fa-question-circle-solid {:tw "w-3 h-3"}]
    [:span {:tw "text-xs"} "WTF?"]]
   [:div.my-balance {:tw "flex items-center gap-1"}
    [ui/token-amount-view @(subscribe [:state/my-balance]) nil]]
   [ui/button {:on-click (fn [] (dispatch [:state/logout!]))}
    [fa/fa-sign-out-alt-solid {:tw "w-3 h-3"}]]])

(defn main-view []
  [:div {:tw "px-8 p-4 max-w-40rem mx-auto"}
   [modal-view]
   [header-view]
   [:div.topics {:tw "space-y-2"}
    (doall
     (for [topic @(subscribe [:state/ranked-topics])]
       ^{:key (:topic/id topic)}
       [topic-view topic]))]])

(defn auth-view []
  (r/with-let
   [state (r/atom ::initial)]
   [:div {:tw "flex flex-col items-center justify-center min-h-screen gap-4"}
    (case @state
      ::initial
      [:form {:on-submit (fn [e]
                           (.preventDefault e)
                           (reset! state ::loading)
                           (-> (state/ajax!
                                {:uri "/api/auth"
                                 :method :post
                                 :params {:email (-> e .-target .-elements (o/get "email") .-value)}})
                               (.then (fn []
                                        (reset! state ::complete)))
                               (.catch (fn []
                                         (reset! state ::error)))))}
       [:input {:type "email"
                :name "email"
                :tw "p-2 border border-gray-300 rounded w-64"
                :placeholder "you@example.com"}]
       [ui/button {:title "Request Login Link"}
        [fa/fa-sign-in-alt-solid {:tw "w-4 h-4"}]]]
      ::loading
      "..."
      ::complete
      "Login link sent. Check your email."
      ::error
      [:div
       "Something went wrong."
       [ui/button {:on-click (fn [] (reset! state ::initial))}
        "Try again"]])]))

(defn app-view []
  [:div {:tw "bg-#edeef3 min-h-screen"}
   [:style
    ;; temporary fix for seizure-inducing scrollbar when popover is active
    "body { overflow-x: hidden }

    .group:hover  .group\\:hover\\:text-gray-600 { color: #4b5563; }"]
   (if @(subscribe [:state/user])
     [main-view]
     [auth-view])])
