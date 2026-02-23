(ns quadvote.ui.group-header
  (:require
   [bloom.commons.pages :as pages]
   [reagent.core :as r]
   [quadvote.ui.state :as state]
   [quadvote.ui.common :as ui]))

(defn group-switcher-view
  [*group]
  (r/with-let
    [*user (state/tada-atom! [:api/user {}])]
    (let [current-group-id (:group/id @*group)
          groups (conj (->> @*user
                            :membership/_user
                            (map :membership/group)
                            (map (fn [group]
                                   (select-keys group [:group/id :group/name])))
                            set)
                       (select-keys (or @*group
                                        {:group/id current-group-id
                                         :group/name "Unknown Group"})
                                    [:group/id :group/name]))]
      [:select {:tw (str ui/input-tw " grow bg-transparent text-sm font-bold")
                :value (str current-group-id)
                :on-change (fn [e]
                             (let [id (.. e -target -value)]
                               (cond
                                 (= "discover" id)
                                 (pages/navigate-to! [:page/discover])
                                 (= "create" id)
                                 (let [name (js/prompt "Name your group:")]
                                   (when (seq name)
                                     (-> (state/tada! [:api/create-group! {:name name}])
                                         (.then (fn [result]
                                                  (state/refresh! *user)
                                                  (pages/navigate-to! [:page/group {:id (:group-id result)}]))))))
                                 :else
                                 (let [selected-group (->> groups
                                                           (filter #(= (str (:group/id %)) id))
                                                           first)]
                                   (pages/navigate-to! [:page/group {:id (:group/id selected-group)}])))))}
       (for [{:group/keys [id name]} (conj groups
                                           {:group/id "discover"
                                            :group/name "Discover other groups..."}
                                           {:group/id "create"
                                            :group/name "Create your own group..."})]
         ^{:key id}
         [:option {:value (str id)} name])])))

(defn header-view
  [{:keys [*group]}]
  [:div.header {:tw "flex justify-between items-center gap-3"}
   [group-switcher-view *group]
   (when (and (nil? (-> @*group :group/membership))
              (-> @*group :group/open-membership?))
     [ui/button {:on-click (fn []
                             (-> (state/tada! [:api/join-group!
                                               {:group-id (:group/id @*group)}])
                                 (.then (fn []
                                          (state/refresh! *group)))))}
      [:span {:tw "text-xs"} "Join Group"]])

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
   (when @*group
     [:<>
      [ui/button {:on-click (fn []
                              (pages/navigate-to! [:page/group {:id (:group/id @*group)}]))}
       [:span {:tw "text-xs"} "Active"]]
      [ui/button {:on-click (fn []
                              (pages/navigate-to! [:page/log {:id (:group/id @*group)}]))}
       [:span {:tw "text-xs"} "Complete"]]])
   (when (-> @*group :group/membership)
     [:div.my-balance {:tw "flex items-center gap-1"}
      [ui/token-amount-view (-> @*group :group/membership :membership/balance) nil]])])
