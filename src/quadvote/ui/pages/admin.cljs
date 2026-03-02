(ns quadvote.ui.pages.admin
  (:require
   [bloom.commons.pages :as pages]
   [goog.object :as o]
   [reagent.core :as r]
   [quadvote.ui.common :as ui]
   [quadvote.ui.form :as form]
   [quadvote.ui.group-common :as group]
   [quadvote.ui.state :as state]))

(defn settings-form-view
  [{:keys [*group]}]
  (r/with-let
    [form (r/atom
           {:name (:group/name @*group)
            :description (:group/description @*group)
            :open-membership? (:group/open-membership? @*group)
            :open-topics? (:group/open-topics? @*group)
            :grant-frequency (:group/grant-frequency @*group)
            :grant-amount (:group/grant-amount @*group)})]
    [:form
     {:tw "space-y-3"
      :on-submit (fn [e]
                   (.preventDefault e)
                   (-> (state/tada!
                        [:api/update-group!
                         (merge {:group-id (:group/id @*group)}
                                @form)])
                       (.then (fn []
                                (state/refresh! *group)))))}
     [:h2 {:tw "font-bold"} "Group Settings"]
     [:label {:tw "block"}
      [:div "Group Name"]
      [:input {:type "text"
               :tw ui/input-tw
               :value (:name @form)
               :on-change #(swap! form assoc :name (-> % .-target .-value))}]]
     [:label {:tw "block"}
      [:div "Description"]
      [:textarea {:tw ui/input-tw
                  :value (:description @form)
                  :on-change #(swap! form assoc :description (-> % .-target .-value))}]]
     [form/radio-list
      {:legend "Membership"
       :options [[false "Private"] [true "Open"]]
       :value (:open-membership? @form)
       :on-change #(swap! form assoc :open-membership? %)}]
     [form/radio-list
      {:legend "Topic Submissions"
       :options [[false "Admins only"] [true "All members"]]
       :value (:open-topics? @form)
       :on-change #(swap! form assoc :open-topics? %)}]
     [form/radio-list
      {:legend "Grant Frequency"
       :options [[:grant-frequency/never "Daily"]
                 [:grant-frequency/daily "Daily"]
                 [:grant-frequency/weekly "Weekly"]
                 [:grant-frequency/monthly "Monthly"]]
       :value (:grant-frequency @form)
       :on-change #(swap! form assoc :grant-frequency %)}]
     [:label {:tw "block"}
      [:div "Grant Amount"]
      [:input {:type "number"
               :tw ui/input-tw
               :value (:grant-amount @form)
               :on-change #(swap! form assoc :grant-amount (js/parseInt (.. % -target -value)))}]]
     [ui/button {} "Save"]]))

(defn memberships-view
  [{:keys [group-id]}]
  (r/with-let
    [*memberships (state/tada-atom! [:api/admin-group-memberships {:group-id group-id}])]
    [:div {:tw "space-y-2"}
     [:h2 {:tw "font-bold"} "Members"]
     [:table {:tw "w-full text-sm"}
      [:thead
       [:tr {:tw "text-left border-b"}
        [:th {:tw "py-1 pr-4"} "Name"]
        [:th {:tw "py-1 pr-4"} "Email"]
        [:th {:tw "py-1 pr-4 text-right"} "Balance"]
        [:th {:tw "py-1 pr-4 text-right"} "Claimable"]
        [:th {:tw "py-1 text-right"} "Admin"]]]
      [:tbody
       (for [{:membership/keys [user balance claimable-token-amount admin? id]} @*memberships]
         ^{:key id}
         [:tr
          [:td {:tw "py-1 pr-4"} (:user/name user)]
          [:td {:tw "py-1 pr-4 text-gray-500"} (:user/email user)]
          [:td {:tw "py-1 pr-4 text-right"} balance]
          [:td {:tw "py-1 pr-4 text-right"} claimable-token-amount]
          [:td {:tw "py-1 text-right"} (when admin? "✓")]])]]]))

(defn view
  [group-id]
  (r/with-let
    [*group (state/tada-atom! [:api/group {:group-id group-id}])]
    [group/page
     {:*group *group}
     [:div {:tw "space-y-8 py-4"}
      (when @*group
        [settings-form-view {:*group *group}])
      [memberships-view {:group-id group-id}]]]))

(pages/register-page!
 {:page/id :page/admin
  :page/view (fn [[_ {:keys [id]}]]
               ^{:key id}
               [view id])
  :page/path "/group/:id/admin"
  :page/parameters {:id :uuid}
  :page/on-enter! (fn [[_ {:keys [id]}]]
                    (reset! state/group-id id))})
