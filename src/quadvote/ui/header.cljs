(ns quadvote.ui.header
  (:require
   [bloom.commons.pages :as pages]
   [bloom.commons.fontawesome :as fa]
   [goog.object :as o]
   [reagent.core :as r]
   [quadvote.ui.state :as state]
   [quadvote.ui.common :as ui]
   [quadvote.ui.modal :as modal]))

(defn new-topic-modal-view
  [*group]
  [:form {:tw "space-y-2"
          :on-submit (fn [e]
                       (.preventDefault e)
                       (-> (state/tada!
                            [:api/create-topic!
                             {:title (-> e .-target .-elements (o/get "title") .-value)
                              :description (-> e .-target .-elements (o/get "description") .-value)
                              :group-id @state/group-id}])
                           (.then (fn []
                                    (state/refresh! *group)
                                    (modal/close!)))))}
   [:h1 {:tw "font-bold"} "Add a Topic"]
   [:label {:tw "block"}
    [:div "Title"]
    [:input {:type "text"
             :name "title"
             :tw ui/input-tw
             :required true
             :auto-focus true}]]
   [:label {:tw "block"}
    [:div "Description"]
    [:textarea {:name "description"
                :tw ui/input-tw
                :required true}]]
   [:div
    [ui/button {} "Submit"]]])

(defn info-modal-view []
  [:div {:tw "prose"}
   [:p "Vote on which projects Raf should spend more time on."]
   [:p]
   [:p "You have tokens to spend across projects."]
   [:p "You can vote multiple times per project, but concentrating votes is more expensive:"]
   [:table
    [:tbody
     [:tr [:th "Tokens =>"] [:th "Votes"]]
     [:tr [:td "1"] [:td "1"]]
     [:tr [:td "4"] [:td "2"]]
     [:tr [:td "9"] [:td "3"]]
     [:tr [:td "16"] [:td "4"]]
     [:tr [:td "25"] [:td "5"]]]]
   [:p "You can adjust your votes any time."]
   [:p "When Raf spends a day on a project, tokens spent on that project will be consumed."]
   [:p "New tokens can be claimed by logging in once a month."]
   [:p "(This is an experiment in applied " [:a {:tw "underline decoration-from-font"
                                                 :target "_blank"
                                                 :href "https://en.wikipedia.org/wiki/Quadratic_voting"} "quadratic voting"] ")"]])

(defn group-switcher-view
  [*group]
  (r/with-let
   [user (state/tada-atom! [:api/user {}])]
   (let [groups (conj (->> @user
                           :membership/_user
                           (map :membership/group)
                           (map (fn [group]
                                  (select-keys group [:group/id :group/name])))
                           set)
                      (select-keys @*group [:group/id :group/name]))
         current-group-id (:group/id @*group)]
     [:select {:tw (str ui/input-tw " grow bg-transparent text-sm font-bold")
               :value (str current-group-id)
               :on-change (fn [e]
                            (let [id (.. e -target -value)]
                              (if-let [path ({"discover" [:page/discover]} id)]
                                (pages/navigate-to! path)
                                (let [selected-group (->> groups
                                                          (filter #(= (str (:group/id %)) id))
                                                          first)]
                                  (pages/navigate-to! [:page/group {:id (:group/id selected-group)}])))))}
      (for [{:group/keys [id name]} (conj groups {:group/id "discover"
                                                  :group/name "Discover other groups..."})]
        ^{:key id}
        [:option {:value (str id)} name])])))

(defn header-view
  [*group]
  [:div.header {:tw "flex justify-between items-center gap-3"}
   [group-switcher-view *group]
   (when (or (-> @*group :group/membership :membership/admin?)
             (and (-> @*group :group/open-topics?)
                  (-> @*group :group/membership)))
     [:div {:tw "text-xs"}
      [ui/button {:on-click (fn []
                              (modal/open! [new-topic-modal-view *group]))}
       [fa/fa-plus-circle-solid {:tw "w-3 h-3"}]
       "Add a Topic"]])
   (when (-> @*group :group/membership :membership/admin?)
     [ui/button {:on-click (fn []
                             (pages/navigate-to! [:page/admin {:id (:group/id @*group)}]))}
      [:span {:tw "text-xs"} "Admin"]])
   (let [amount (-> @*group :group/membership :membership/claimable-token-amount)]
     (when (< 0 (or amount 0))
       [:div
        [:span {:tw "text-xs"}
         "Claim your bonus → "]
        [:button {:on-click (fn []
                              (-> (state/tada!
                                   [:api/claim!
                                    {:membership-id (-> @*group :group/membership :membership/id)}])
                                  (.then (fn [_]
                                           (state/refresh! *group)))))}
         [ui/token-amount-view amount :gain]]]))
   [ui/button {:on-click (fn []
                           (pages/navigate-to! [:page/group {:id (:group/id @*group)}]))}
    [:span {:tw "text-xs"} "Active"]]
   [ui/button {:on-click (fn []
                           (pages/navigate-to! [:page/log {:id (:group/id @*group)}]))}
    [:span {:tw "text-xs"} "Complete"]]
   [ui/button {:on-click (fn [] (modal/open! [info-modal-view]))}
    [fa/fa-question-circle-solid {:tw "w-3 h-3"}]
    [:span {:tw "text-xs"} "WTF?"]]
   (when (-> @*group :group/membership)
     [:div.my-balance {:tw "flex items-center gap-1"}
      [ui/token-amount-view (-> @*group :group/membership :membership/balance) nil]])
   [ui/button {:on-click (fn []
                           (when (js/confirm "Log out?")
                             (state/ajax!
                              {:uri "/api/auth"
                               :method :delete
                               :on-success (fn [_]
                                             (js/window.location.reload))
                               :on-error (fn [])})))}
    [fa/fa-sign-out-alt-solid {:tw "w-3 h-3"}]]])
