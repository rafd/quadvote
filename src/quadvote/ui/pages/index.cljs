(ns quadvote.ui.pages.index
  (:require
   [bloom.commons.pages :as pages]
   [reagent.core :as r]
   [quadvote.ui.state :as state]))

(defn view
  [_]
  (r/with-let
   [user (state/tada-atom! [:api/user {}])]
    (let [groups (->> @user
                      :membership/_user
                      (map :membership/group))]
      [:div
       (for [{:group/keys [id name]} groups]
         ^{:key id}
         [:div
          [:a {:href (pages/path-for [:page/group {:id id}])}
           name]])
       (when (= (count groups) 1)
         (pages/navigate-to! [:page/group {:id (:group/id (first groups))}]))])))

(pages/register-page!
 {:page/id :page/index
  :page/view #'view
  :page/path "/"})
