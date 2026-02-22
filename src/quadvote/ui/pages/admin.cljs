(ns quadvote.ui.pages.admin
  (:require
   [goog.object :as o]
   [reagent.core :as r]
   [bloom.commons.pages :as pages]
   [quadvote.ui.common :as ui]
   [quadvote.ui.form :as form]
   [quadvote.ui.group-common :as group]
   [quadvote.ui.state :as state]))

#_(defn invite-form-view
  [{:keys [membership]}]
  (r/with-let [status (r/atom nil)]
    [:form
     {:tw "space-y-2"
      :on-submit (fn [e]
                   (.preventDefault e)
                   (reset! status ::loading)
                   (-> (state/tada!
                        [:api/add-user-to-group!
                         {:name (-> e .-target .-elements (o/get "name") .-value)
                          :email (-> e .-target .-elements (o/get "email") .-value)
                          :group-id (get-in @membership [:membership/group :group/id])}])
                       (.then (fn []
                                (reset! status ::done)
                                (.reset (.-target e))))
                       (.catch (fn []
                                 (reset! status ::error)))))}
     [:h2 {:tw "font-bold"} "Invite User"]
     [:label {:tw "block"}
      [:div "Name"]
      [:input {:type "text"
               :name "name"
               :tw ui/input-tw
               :required true}]]
     [:label {:tw "block"}
      [:div "Email"]
      [:input {:type "email"
               :name "email"
               :tw ui/input-tw
               :required true}]]
     [ui/button {} "Invite"]
     (case @status
       ::loading "..."
       ::done "Invited!"
       ::error "Something went wrong."
       nil)]))

(defn settings-form-view
  [{:keys [membership]}]
  (r/with-let
   [form (r/atom
          (let [group (:membership/group @membership)]
            {:name (:group/name group)
             :open-membership? (:group/open-membership? group)
             :open-topics? (:group/open-topics? group)
             :grant-frequency (:group/grant-frequency group)
             :grant-amount (:group/grant-amount group)}))]
   [:form
    {:tw "space-y-3"
     :on-submit (fn [e]
                  (.preventDefault e)
                  (-> (state/tada!
                       [:api/update-group!
                        (merge {:group-id (get-in @membership [:membership/group :group/id])}
                               @form)])
                      (.then (fn []
                               (state/refresh! membership)))))}
    [:h2 {:tw "font-bold"} "Group Settings"]
    [:label {:tw "block"}
     [:div "Group Name"]
     [:input {:type "text"
              :tw ui/input-tw
              :value (:name @form)
              :on-change #(swap! form assoc :name (-> % .-target .-value))}]]
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

(defn view
  [group-id]
  (r/with-let
   [membership (state/tada-atom! [:api/membership {:group-id group-id}])]
   [group/page
    {:membership membership}
    [:div {:tw "space-y-8"}
     (when @membership
       [:<>
        #_[invite-form-view {:membership membership}]
        [settings-form-view {:membership membership}]])]]))

(pages/register-page!
 {:page/id :page/admin
  :page/view (fn [[_ {:keys [id]}]]
               ^{:key id}
               [view id])
  :page/path "/group/:id/admin"
  :page/parameters {:id :uuid}
  :page/on-enter! (fn [[_ {:keys [id]}]]
                    (reset! state/group-id id))})
