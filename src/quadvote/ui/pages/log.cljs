(ns quadvote.ui.pages.log
  (:require
   [bloom.commons.pages :as pages]
   [reagent.core :as r]
   [quadvote.ui.group-common :as group]
   [quadvote.ui.common :as ui]
   [quadvote.ui.state :as state]))

(defn format-timestamp
  [timestamp]
  (.toLocaleDateString
   (js/Date. timestamp)
   "en-CA"
   #js {:year "numeric"
        :month "numeric"
        :day "numeric"}))

(defn burns-view
  [membership]
  [:div.burns
   (for [topic (->> @membership
                    :membership/group
                    :topic/_group
                    (filter :topic/burn)
                    (sort-by (fn [t]
                               (-> t :topic/burn :burn/timestamp)) >))

         :let [{:burn/keys [id timestamp message user]}
               (:topic/burn topic)]]
     ^{:key id}
     [:div {:tw "p-4 mb-2 bg-white space-y-2 rounded shadow"}
      [:div.title {:tw "font-bold"}
       (:topic/title topic)]
      [:div.message {:tw "whitespace-pre-wrap text-sm"}
       message]
      [:div.footer {:tw "flex gap-1 items-center justify-end"}
       [ui/user-circles [user]]
       [:div.timestamp {:tw "text-xs text-gray-500"}
        (format-timestamp timestamp)]]])])

(defn view
  [[_ {:keys [id]}]]
  (r/with-let
   [membership (state/tada-atom! [:api/membership {:group-id id}])]
   [group/page
    {:membership membership}
    [burns-view membership]]))

(pages/register-page!
 {:page/id :page/log
  :page/view #'view
  :page/path "/group/:id/log"
  :page/parameters {:id :uuid}
  :page/on-enter! (fn [[_ {:keys [id]}]]
                    (reset! state/group-id id))})
