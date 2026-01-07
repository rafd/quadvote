(ns quadvote.ui.pages.group
  (:require
   [bloom.commons.pages :as pages]
   [reagent.core :as r]
   [bloom.commons.fontawesome :as fa]
   [bloom.commons.debounce :as debounce]
   [quadvote.ui.common :as ui]
   [markdown.core :as md]
   [quadvote.model :as model]
   [quadvote.ui.state :as state]
   [quadvote.ui.modal :as modal]))

(defn voting-controls-linear-view [vote]
  [:div.votes {:tw "flex flex-col space-y-3px min-w-3"}
   (for [i (range 0 (:vote/voice-amount vote))]
     ^{:key i}
     [:div.bar {:tw "w-3 h-0.75 bg-green-700 rounded"}])])

(defn topic-view
  [{:keys [membership user]} topic]
  (r/with-let
   [show-description? (r/atom false)
    vote! (fn [{:keys [topic-id new-voice-amount]}]
            (-> (state/tada! [:api/vote!
                              {:topic-id topic-id
                               :voice-amount new-voice-amount}])
                (.then (fn []
                         (state/refresh! membership)))))]
   [:div.topic {:tw "bg-white rounded overflow-hidden shadow"}
    [:div.main {:tw "flex justify-between items-center"}
     [:div.title {:tw "p-4 flex flex-wrap justify-between grow gap-2 items-center cursor-pointer group"
                  :on-click (fn []
                              (swap! show-description? not))}
      (:topic/title topic)
      [fa/fa-info-circle-solid {:tw "w-3 h-3 text-gray-400 group:hover:text-gray-600"}]

      [:div {:tw "grow"}]

      [:div.supporters {:tw "flex -space-x-1.5"}
       (for [{:user/keys [id name]} (->> topic
                                         :vote/_topic
                                         (map (fn [vote]
                                                (:vote/user vote)))
                                         (sort-by :user/id))]
         ^{:key id}
         [:div {:tw "rounded-full text-white text-center text-xs w-5 h-5 leading-5 border border-white"
                :title name
                :style {:background (ui/color id)}}
          (first name)])]]

     (let [vote (state/topic->user-vote topic (:user/id @user))
           can-upvote? (and
                        (< (:vote/voice-amount vote) model/max-voice-amount-per-vote)
                        (model/can-afford?
                         {:balance
                          (:membership/balance @membership)
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
      [:div.description
       {:tw "px-4 whitespace-pre-wrap prose text-xs border-t border-gray-300"
        :dangerouslySetInnerHTML
        {:__html
         (md/md->html
          (:topic/description topic))}}])]))

(defn header-view
  [membership]
  [:div.header {:tw "flex justify-between items-center pb-4 gap-3"}
   [:h1 {:tw "grow"}
    (:group/name (:membership/group membership))]
   #_[ui/button {:on-click (fn [] (modal/open! :modal/new-topic))}
      [fa/fa-plus-circle-solid {:tw "w-4 h-4"}]
      "Suggest a Topic"]
   [ui/button {:on-click (fn [] (modal/open! :modal/info))}
    [fa/fa-question-circle-solid {:tw "w-3 h-3"}]
    [:span {:tw "text-xs"} "WTF?"]]
   [:div.my-balance {:tw "flex items-center gap-1"}
    [ui/token-amount-view (:membership/balance membership) nil]]
   [ui/button {:on-click (fn []
                           (state/ajax!
                            {:uri "/api/auth"
                             :method :delete
                             :on-success (fn [_]
                                           (js/window.location.reload))
                             :on-error (fn [])}))}
    [fa/fa-sign-out-alt-solid {:tw "w-3 h-3"}]]])

(defn view
  [[_ {:keys [id]}]]
  (r/with-let
   [membership (state/tada-atom! [:api/membership {:group-id id}])
    user (state/tada-atom! [:api/user {}])
    id->topic (r/reaction
               (let [topics (-> @membership :membership/group :topic/_group)]
                 (zipmap (map :topic/id topics)
                         topics)))
    ;; update order of topics with a delay
    ;; for a better user experience
    sorted-topic-ids (r/atom [])
    resort-topics! (debounce/debounce
                    (fn [topics]
                      (reset! sorted-topic-ids
                              (->> topics
                                   shuffle ;; force random order for equal scoring topics
                                   (sort-by state/topic->total-voice-amount >)
                                   (map :topic/id))))
                    750)
    _ (r/track!
       (fn []
         (resort-topics! (-> @membership :membership/group :topic/_group))))]
   [:div {:tw "px-8 p-4 max-w-40rem mx-auto"}
    [modal/modal-view]
    [header-view @membership]
    [:div.topics {:tw "space-y-2"}
     (let [->topic @id->topic]
      (for [topic (->> @sorted-topic-ids
                       (map ->topic))]
        ^{:key (:topic/id topic)}
        [topic-view
         {:membership membership
          :user user}
         topic]))]]))

(pages/register-page!
 {:page/id :page/group
  :page/view #'view
  :page/path "/group/:id"
  :page/parameters {:id :uuid}})
