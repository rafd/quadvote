(ns quadvote.ui
  (:require
    [bloom.commons.uuid :as uuid]
    [bloom.commons.fontawesome :as fa]
    [bloom.commons.tada.rpc.client :as tada.rpc]
    [bloom.commons.ajax :as ajax]
    [bloom.omni.fx.dispatch-debounce :as debounce]
    [goog.object :as o]
    [re-frame.core :refer [dispatch subscribe reg-sub reg-event-fx reg-fx]]
    [reagent.core :as r]
    [quadvote.ui.common :as ui]
    [quadvote.state :as state]))

(defn key-by [f coll]
  (zipmap (map f coll)
          coll))

(reg-fx :tada (tada.rpc/make-dispatch {:base-path "/api/tada"}))

(reg-fx :ajax ajax/request)

(reg-fx :dispatch-debounce debounce/fx)

(reg-event-fx :state/initialize!
  (fn [{db :db} _]
    {:db {:client.db/topics {}
          :client.db/topic-voice-amounts {}
          :client.db/me nil
          :client.db/my-balance nil
          :client.db/my-votes {}
          :client.db/modal nil}
     :dispatch [::check-auth!]}))

(reg-event-fx ::check-auth!
  (fn [{db :db} _]
    {:ajax {:uri "/api/auth"
            :method :get
            :on-success (fn [_]
                          (dispatch [::fetch-data!]))
            :on-error (fn [])}}))

(reg-event-fx ::fetch-data!
  (fn [{db :db} _]
    {:tada [:api/data
            {}
            {:on-success
             (fn [data]
               (dispatch [::store-data! data]))}]}))

(reg-event-fx ::resort!
  (fn [{db :db} _]
    {:db (assoc db :client.db/sorted-topic-ids
           (->> db
                :client.db/topics
                vals
                (sort-by (fn [topic]
                           (get-in db [:client.db/topic-voice-amounts (:topic/id topic)])))
                (map :topic/id)
                reverse))}))

(reg-event-fx ::store-data!
  (fn [{db :db} [_ data]]
    {:db {:client.db/topics (key-by :topic/id (:api.data/topics data))
          :client.db/topic-voice-amounts (:api.data/topic-voice-amounts data)
          :client.db/sorted-topic-ids
          (->> (:api.data/topics data)
               (sort-by (fn [topic]
                          (get-in data [:api.data/topic-voice-amounts (:topic/id topic)])))
               (map :topic/id)
               reverse)
          :client.db/me (:api.data/user data)
          :client.db/my-balance (:api.data/balance data)
          :client.db/my-votes (key-by :vote/topic-id (:api.data/votes data))}}))

(reg-event-fx :state/vote!
  (fn [{db :db} [_ vote topic-id previous-voice-amount new-voice-amount]]
    (let [vote (if vote
                (assoc vote :vote/voice-amount new-voice-amount)
                {:vote/id (uuid/random)
                 :vote/voice-amount new-voice-amount
                 :vote/user-id nil ;; TODO
                 :vote/topic-id topic-id})]
      {:db (-> (if (= 0 new-voice-amount)
                 (update db :client.db/my-votes dissoc topic-id)
                 (assoc-in db [:client.db/my-votes topic-id] vote))
               (update-in [:client.db/topic-voice-amounts topic-id]
                          + (- new-voice-amount
                               previous-voice-amount))
               (update :client.db/my-balance
                       - (state/token-cost previous-voice-amount new-voice-amount)))
       :dispatch-debounce [{:id ::resort
                            :dispatch [::resort!]
                            :timeout 750}]
       :tada [:api/vote!
              {:vote-id (:vote/id vote)
               :topic-id topic-id
               :voice-amount new-voice-amount}
              {:on-success (fn [_]
                             #_(dispatch [::fetch-data!]))}]})))

(reg-event-fx :state/open-modal!
  (fn [{db :db} [_ modal-id]]
    {:db (assoc db :client.db/modal modal-id)}))

(reg-event-fx :state/create-topic!
  (fn [{db :db} [_ text]]
    {:tada [:api/create-topic!
            {:id (uuid/random)
             :text text}
            {:on-success
             (fn [_]
               (dispatch [::fetch-data!]))}]}))

(reg-sub :state/modal
  (fn [db _]
    (:client.db/modal db)))

(reg-sub :state/ranked-topics
  (fn [db _]
    (->> (:client.db/sorted-topic-ids db)
         (map (fn [topic-id]
                (get-in db [:client.db/topics topic-id]))))
    #_(->> (:client.db/topics db)
         vals
         (sort-by (fn [topic]
                    (get-in db [:client])
                    ))
         #_(sort-by (fn [topic]
                    (get-in db [:client.db/topic-voice-amounts (:topic/id topic)])))
         reverse)))

(reg-sub :state/topic-voice-amount
  (fn [db [_ topic-id]]
    (get-in db [:client.db/topic-voice-amounts topic-id] 0)))

(reg-sub :state/my-balance
  (fn [db _]
    (:client.db/my-balance db)))

(reg-sub :state/vote-for-topic
  (fn [db [_ topic-id]]
    (get-in db [:client.db/my-votes topic-id])))

(def tw-button
  "flex gap-1 items-center border border-gray-300 p-1 rounded")

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
  [:div
   [:p "Vote on what topic to discuss at the next meeting."]
   [:p "You have tokens to spend across topics."]
   [:p "You can vote multiple times per topic, but concentrating votes is more expensive (1, 4, 9, 16, 25). Max 5 per topic. You can adjust your vote any time."]
   [:p "When a topic is finalized, tokens spent on votes for that topic are burned."]
   [:p "Earn new tokens by attending meetings."]
   [:p "(This is an experiment in applied " [:a {:tw "underline decoration-from-font"
                                                 :href "https://en.wikipedia.org/wiki/Quadratic_voting"} "quadratic voting"] ")"]])

(defn claim-tokens-modal-view [])

(defn modal-view []
  (when-let [modal-id @(subscribe [:state/modal])]
    [:div.wrapper {:tw "absolute top-0 right-0 left-0 bottom-0 flex p-10 cursor-pointer items-center justify-center"
                   :on-click (fn []
                               (dispatch [:state/open-modal! nil]))}
     [:div.modal {:tw "shadow bg-white rounded h-full w-full max-w-40rem p-10 cursor-default"
                  :on-click-capture (fn [e]
                                      (.stopPropagation e))}
      (case modal-id
        :modal/new-topic
        [new-topic-modal-view]
        :modal/info
        [info-modal-view]
        :modal/claim-tokens
        [claim-tokens-modal-view])]]))

(defn voting-controls-linear-view [vote]
  [:div.votes {:tw "flex flex-col space-y-3px min-w-3"}
   (for [i (range 0 (:vote/voice-amount vote))]
     ^{:key i}
     [:div.bar {:tw "w-3 h-0.75 bg-green-700 rounded"}])])

(defn gem-amount-view [amount ?prefix]
  [:div {:tw "flex gap-0.5 items-center rounded px-1 justify-end"
         :style {:background-color "rgba(0,0,0,0.05)"}}
   [:span {:style {:font-family "monospace"}}
    (case ?prefix
      :refund
      "⤴"
      :cost
      [:div {:style {:transform "rotate(90deg)"}} "⤵"]
      :current
      "="
      nil)]
   [:span amount] [ui/gem-icon]])

(defn topic-view
  [topic]
  [:div.topic {:tw "bg-white rounded overflow-hidden flex justify-between items-center shadow"}
   [:div.text {:tw "p-4"} (:topic/text topic)]
   (let [vote @(subscribe [:state/vote-for-topic (:topic/id topic)])
         can-upvote? (and
                      (< (:vote/voice-amount vote) state/max-voice-amount-per-vote)
                      (state/can-afford? @(subscribe [:state/my-balance])
                                         (:vote/voice-amount vote) (inc (:vote/voice-amount vote))))]
     [:div.meta {:tw ["flex px-2 items-center gap-1 self-stretch"
                      (if vote
                        "bg-green-200"
                        "bg-gray-200")]}
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
          (for [i (reverse (range 0 state/max-voice-amount-per-vote))]
            ^{:key i}
            [:rect {:x 0
                    :y 0
                    :height (* (/ s state/max-voice-amount-per-vote) (inc i))
                    :width (* (/ s state/max-voice-amount-per-vote) (inc i))
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
           (let [cost (- (* (inc (:vote/voice-amount vote))
                            (inc (:vote/voice-amount vote)))
                         (* (:vote/voice-amount vote)
                            (:vote/voice-amount vote)))]
             [:span {:title (str "Increasing your vote by 1 costs you " cost " gems")}
              [gem-amount-view cost :cost]])]
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
                           (* (:vote/voice-amount vote) (:vote/voice-amount vote)) " gems")}
         [gem-amount-view (* (:vote/voice-amount vote) (:vote/voice-amount vote)) :current]]
        [:div {:tw "w-4 text-center"}
         (:vote/voice-amount vote)]]

       ;; decrease-vote
       (if vote
         [ui/popover
          {:position :right}
          (let [refund (- (* (:vote/voice-amount vote)
                             (:vote/voice-amount vote))
                          (* (dec (:vote/voice-amount vote))
                             (dec (:vote/voice-amount vote))))]
            [:div {:title (str "Reducing your vote by 1 refunds you " refund " gems")}
             [gem-amount-view refund :refund]])
          [:button {:tw "px-1 flex gap-1"
                    :on-click (fn [_]
                                (dispatch [:state/vote!
                                           vote
                                           (:topic/id topic)
                                           (:vote/voice-amount vote)
                                           (dec (:vote/voice-amount vote))]))}
           [fa/fa-caret-down-solid {:tw "w-4 h-4"}]]]
         [:div {:tw "px-1 w-4 h-4"}])]])])

(defn app-view []
  [:div {:tw "bg-#edeef3"}
   [:div {:tw " p-4 max-w-40rem mx-auto"}
    [modal-view]
    [:div.header {:tw "flex justify-between items-center pb-4 gap-3"}
     [:h1 {:tw "grow"} "Next Discussions"]
     [:button {:tw tw-button
               :on-click (fn [] (dispatch [:state/open-modal! :modal/new-topic]))}
      [fa/fa-plus-circle-solid {:tw "w-4 h-4"}] "Suggest a Topic"]
     [:button {:tw tw-button
               :on-click (fn [] (dispatch [:state/open-modal! :modal/info]))}
      [fa/fa-question-circle-solid {:tw "w-4 h-4"}] "WTF?"]
     [:button {:tw tw-button
               :on-click (fn [] (dispatch [:state/open-modal! :modal/claim-tokens]))}
      [fa/fa-plus-circle-solid {:tw "w-4 h-4"}] "Claim"]
     [:div.my-balance {:tw "flex items-center gap-1"}
      @(subscribe [:state/my-balance])
      [ui/gem-icon]]]
    [:div.topics {:tw "space-y-2"}
     (doall
       (for [topic @(subscribe [:state/ranked-topics])]
         ^{:key (:topic/id topic)}
         [topic-view topic]))]]])
