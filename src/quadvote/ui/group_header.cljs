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
      [:select {:tw (str ui/input-tw " grow bg-transparent text-sm font-bold border border-gray-400")
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
  [:div.header {:tw "bg-#e0e1e9 border-b border-gray-400"}
   [:div.content {:tw "max-w-40rem mx-auto flex justify-between items-center gap-3 px-4 py-2"}
    [group-switcher-view *group]
    (when (and (nil? (-> @*group :group/membership))
               (-> @*group :group/open-membership?))
      [ui/button {:on-click (fn []
                              (-> (state/tada! [:api/join-group!
                                                {:group-id (:group/id @*group)}])
                                  (.then (fn []
                                           (state/refresh! *group)))))}
       [:span {:tw "text-xs"} "Join Group"]])

    [:div {:tw "grow"}]

    ;; sub-pages
    [:div {:tw "flex gap-2"}
     (for [[label path] (->> [(when @*group
                                ["Vote" [:page/group {:id (:group/id @*group)}]])
                              (when @*group
                                ["Log" [:page/log {:id (:group/id @*group)}]])
                              (when (-> @*group :group/membership :membership/admin?)
                                ["Admin" [:page/admin {:id (:group/id @*group)}]])]
                             (remove nil?))]
       [:a {:href (pages/path-for path)
            :tw ["px-3 pb-1 pt-1.5 rounded-t border-t border-x border-gray-400 -mb-4"
                 (when (pages/active? path)
                   "bg-#edeef3")]}
        label])]

    ;; claimable grant
    (let [amount (-> @*group :group/membership :membership/claimable-token-amount)]
      (when (< 0 (or amount 0))
        [:div {:tw "absolute bottom-0 left-0 right-0 flex items-center justify-center pointer-events-none"}
         [:div {:tw "pointer-events-auto bg-black/10 rounded px-3 py-1.5 flex items-center gap-2 mb-2"}
          [:span {:tw "text-xs"}
           "Claim your new tokens → "]
          [:button {:on-click (fn []
                                (-> (state/tada!
                                     [:api/claim!
                                      {:membership-id (-> @*group :group/membership :membership/id)}])
                                    (.then (fn [_]
                                             (state/refresh! *group)))))}
           [ui/token-amount-view amount :gain]]]]))

    ;; token balance
    (when (-> @*group :group/membership)
      [:div.my-balance {:tw "flex items-center gap-1 -mb-3"}
       [ui/token-amount-view (-> @*group :group/membership :membership/balance) nil]])]])
