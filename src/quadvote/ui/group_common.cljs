(ns quadvote.ui.group-common
  (:require
   [quadvote.ui.group-header :as group-header]))

(defn page
  [{:keys [*group]} content]
  [:div {:tw "px-8 p-4 max-w-40rem mx-auto"}
   [group-header/header-view {:*group *group}]
   content])
