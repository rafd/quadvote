(ns quadvote.ui.pages.index
  (:require
   [bloom.commons.pages :as pages]
   [reagent.core :as r]
   [quadvote.ui.state :as state]))

(defn view
  [_]
  (r/with-let
    [*user (state/tada-atom! [:api/user {}])]
    (let [groups (->> @*user
                      :membership/_user
                      (map :membership/group))]
      [:div
       (if (seq groups)
         (pages/navigate-to! [:page/group {:id (:group/id (first groups))}])
         (pages/navigate-to! [:page/discover {}]))])))

(pages/register-page!
 {:page/id :page/index
  :page/view #'view
  :page/path "/"})
