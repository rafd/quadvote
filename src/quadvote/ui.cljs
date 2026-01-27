(ns quadvote.ui
  (:require
    [bloom.commons.fontawesome :as fa]
    [bloom.commons.pages :as pages]
    [goog.object :as o]
    [reagent.core :as r]
    [quadvote.ui.pages.group]
    [quadvote.ui.pages.log]
    [quadvote.ui.pages.index]
    [quadvote.ui.common :as ui]
    [quadvote.ui.state :as state]))

(defn auth-view []
  (r/with-let
   [state (r/atom ::initial)]
   [:div {:tw "flex flex-col items-center justify-center min-h-screen gap-4"}
    (case @state
      ::initial
      [:form {:on-submit (fn [e]
                           (.preventDefault e)
                           (reset! state ::loading)
                           (-> (state/ajax!
                                {:uri "/api/auth"
                                 :method :post
                                 :params {:email (-> e .-target .-elements (o/get "email") .-value)}})
                               (.then (fn []
                                        (reset! state ::complete)))
                               (.catch (fn []
                                         (reset! state ::error)))))}
       [:input {:type "email"
                :name "email"
                :tw "p-2 border border-gray-300 rounded w-64"
                :placeholder "you@example.com"}]
       [ui/button {:title "Request Login Link"}
        [fa/fa-sign-in-alt-solid {:tw "w-4 h-4"}]]]
      ::loading
      "..."
      ::complete
      "Login link sent. Check your email."
      ::error
      [:div
       "Something went wrong."
       [ui/button {:on-click (fn [] (reset! state ::initial))}
        "Try again"]])]))

(defn app-view []
  (r/with-let
   [user (state/tada-atom! [:api/user {}])]
   [:div {:tw "bg-#edeef3 min-h-screen"}
    [:style
     ;; temporary fix for seizure-inducing scrollbar when popover is active
     "body { overflow-x: hidden }

     .group:hover  .group\\:hover\\:text-gray-600 { color: #4b5563; }"]
    (if (nil? @user)
      [auth-view]
      [pages/current-page-view])]))
