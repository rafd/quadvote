(ns quadvote.ui.group-common
  (:require
   [quadvote.ui.group-header :as group-header]))

(defn page
  [{:keys [*group]} content]
  [:div
   [group-header/header-view {:*group *group}]
   [:div {:tw "max-w-40rem mx-auto px-2 desktop:px-4"}
    content]])
