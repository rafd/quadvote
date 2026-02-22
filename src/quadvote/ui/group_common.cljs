(ns quadvote.ui.group-common
  (:require
   [quadvote.ui.modal :as modal]
   [quadvote.ui.header :as header]))

(defn page
  [{:keys [*group]} content]
  [:div {:tw "px-8 p-4 max-w-40rem mx-auto"}
   [modal/modal-view]
   [header/header-view *group]
   content])
