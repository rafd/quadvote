(ns quadvote.ui.pages.group
  (:require
   [clojure.string :as string]
   [bloom.commons.pages :as pages]
   [bloom.commons.fontawesome :as fa]
   [bloom.commons.debounce :as debounce]
   [markdown.core :as md]
   [reagent.core :as r]
   [quadvote.ui.group-common :as group]
   [quadvote.ui.common :as ui]
   [quadvote.model :as model]
   [quadvote.ui.state :as state]
   [quadvote.ui.modal :as modal]))

(defn voting-controls-linear-view [vote]
  [:div.votes {:tw "flex flex-col space-y-3px min-w-3"}
   (for [i (range 0 (:vote/voice-amount vote))]
     ^{:key i}
     [:div.bar {:tw "w-3 h-0.75 bg-green-700 rounded"}])])

(defn topic-view
  [{:keys [*group *user]} topic]
  (r/with-let
    [show-description? (r/atom false)
     vote! (fn [{:keys [topic-id new-voice-amount]}]
             (-> (state/tada! [:api/vote!
                               {:topic-id topic-id
                                :voice-amount new-voice-amount}])
                 (.then (fn []
                          (state/refresh! *group)))))]
    [:div.topic {:tw "bg-white rounded overflow-hidden shadow"}
     [:div.main {:tw "flex justify-between items-center"}
      [:div.title {:tw "p-4 flex flex-wrap justify-between grow gap-2 items-center cursor-pointer group"
                   :on-click (fn []
                               (swap! show-description? not))}
       (:topic/title topic)
       [fa/fa-info-circle-solid {:tw "w-3 h-3 text-gray-400 group:hover:text-gray-600"}]

       [:div {:tw "grow"}]

       [ui/user-circles (->> topic
                             :vote/_topic
                             (map (fn [vote]
                                    (:vote/user vote)))
                             (sort-by :user/id))]]

      (let [vote (state/topic->user-vote topic (:user/id @*user))
            can-upvote? (and
                         (< (:vote/voice-amount vote) model/max-voice-amount-per-vote)
                         (model/can-afford?
                          {:balance
                           (-> @*group :group/membership :membership/balance)
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
          (state/topic->total-voice-amount topic)]

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
                                   (vote! {:topic-id (:topic/id topic)
                                           :new-voice-amount (inc (:vote/voice-amount vote))}))}
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
                                   (vote! {:topic-id (:topic/id topic)
                                           :new-voice-amount (dec (:vote/voice-amount vote))}))}
              [fa/fa-caret-down-solid {:tw "w-4 h-4"}]]]
            [:div {:tw "px-1 w-4 h-4"}])]])]
     (when @show-description?
       [:div.extra {:tw "px-4 border-t border-gray-300 flex"}
        [:div.description
         {:tw "whitespace-pre-wrap text-xs prose grow"
          :dangerouslySetInnerHTML
          (r/unsafe-html (md/md->html
                          (:topic/description topic)))}]
        [:div.actions {:tw "py-4"}
         (when (-> @*group :group/membership :membership/admin?)
           [ui/button {:on-click (fn []
                                   (let [message (js/prompt "Message:")]
                                     (when-not (string/blank? message)
                                       (-> (state/tada! [:api/burn-topic!
                                                         {:topic-id (:topic/id topic)
                                                          :message message}])
                                           (.then (fn []
                                                    (state/refresh! *group)))))))}
            "Burn"])]])]))

(defn view
  [group-id]
  (r/with-let
    [*group (state/tada-atom! [:api/group {:group-id group-id}])
     *user (state/tada-atom! [:api/user {}])
     id->topic (r/reaction
                (let [topics (-> @*group :topic/_group)]
                  (zipmap (map :topic/id topics)
                          topics)))
    ;; update order of topics with a delay (for a better user experience)
     sorted-topic-ids (r/atom [])
     resort-topics! (fn [topics]
                      (reset! sorted-topic-ids
                              (->> topics
                                   shuffle ;; force random order for equal scoring topics
                                   (sort-by state/topic->total-voice-amount >)
                                   (map :topic/id))))
     resort-topics-debounced! (debounce/debounce resort-topics! 750)
    ;; we can't immediately resort, because the list is empty
    ;; rely on the track to pick it up, but don't delay on the first time
     initialized? (atom false)
     _ (r/track!
        (fn []
          (let [topics (-> @*group :topic/_group)]
            (when (seq topics)
              (if @initialized?
                (resort-topics-debounced! topics)
                (do
                  (reset! initialized? true)
                  (resort-topics! topics)))))))]
    [group/page
     {:*group *group}
     [:<>
      (if-let [description (-> @*group :group/description)]
        [:div.description
         {:tw "prose text-sm"
          :dangerouslySetInnerHTML
          (r/unsafe-html (md/md->html description))}]
        [:div.spacer {:tw "h-4"}])
      [:div.topics {:tw "space-y-2"}
       (let [->topic @id->topic]
         (for [topic (->> @sorted-topic-ids
                          (map ->topic)
                          (remove :topic/burn))]
           ^{:key (:topic/id topic)}
           [topic-view
            {:*group *group
             :*user *user}
            topic]))]]]))

(pages/register-page!
 {:page/id :page/group
  :page/view (fn [[_ {:keys [id]}]]
               ^{:key id}
               [view id])
  :page/path "/group/:id"
  :page/parameters {:id :uuid}
  :page/on-enter! (fn [[_ {:keys [id]}]]
                    (reset! state/group-id id))})
