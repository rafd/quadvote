(ns quadvote.ui.pages.discover
  (:require
   [bloom.commons.pages :as pages]
   [reagent.core :as r]
   [quadvote.ui.common :as ui]
   [quadvote.ui.state :as state]))

(defn view [_]
  (r/with-let
    [public-groups (state/tada-atom! [:api/public-groups {}])]
    [:div {:tw "px-8 p-4 max-w-40rem mx-auto space-y-4"}
     [:h1 {:tw "font-bold text-lg"} "Discover Groups"]
     (cond
       (nil? @public-groups)
       "Loading..."

       (empty? @public-groups)
       [:p "No public groups available."]

       :else
       [:ul {:tw "space-y-2"}
        (for [{:group/keys [id name]} @public-groups]
          ^{:key id}
          [:li
           [ui/button {:on-click (fn []
                                   (pages/navigate-to! [:page/group {:id id}]))}
            name]])])]))

(pages/register-page!
 {:page/id :page/discover
  :page/view #'view
  :page/path "/discover"})
