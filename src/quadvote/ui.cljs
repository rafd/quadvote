(ns quadvote.ui
  (:require
   [bloom.commons.pages :as pages]
   [reagent.core :as r]
   [quadvote.ui.gem-animation :as gem-animation]
   [quadvote.ui.header :as header]
   [quadvote.ui.modal :as modal]
   [quadvote.ui.pages.admin]
   [quadvote.ui.pages.discover]
   [quadvote.ui.pages.group]
   [quadvote.ui.pages.index]
   [quadvote.ui.pages.log]
   [quadvote.ui.state :as state]))

(defn app-view []
  (r/with-let
    [*user (state/tada-atom! [:api/user {}])]
    [:div {:tw "bg-#edeef3 min-h-screen"}
     [:style
     ;; temporary fix for seizure-inducing scrollbar when popover is active
      "body { overflow-x: hidden }

     .group:hover  .group\\:hover\\:text-gray-600 { color: #4b5563; }"]
     [gem-animation/overlay-view]
     [modal/modal-view]
     [header/header-view {:*user *user}]
     [pages/current-page-view]]))
