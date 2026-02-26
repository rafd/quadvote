(ns quadvote.ui.header
  (:require
   [bloom.commons.fontawesome :as fa]
   [quadvote.ui.common :as ui]
   [quadvote.ui.modal :as modal]
   [quadvote.ui.state :as state]))

(defn info-modal-view []
  [:div {:tw "prose"}
   [:p {:style {:margin-top 0}} "QuadVote lets groups vote on topics over time. For example, 'What should we watch on movie night?'"]
   [:p]
   [:p "As a member, you have tokens to spend across topics."]
   [:p "You can vote multiple times per topic, but concentrating votes is more expensive:"]
   [:table
    [:tbody
     [:tr [:th "Tokens =>"] [:th "Votes"]]
     [:tr [:td "1"] [:td "1"]]
     [:tr [:td "4"] [:td "2"]]
     [:tr [:td "9"] [:td "3"]]
     [:tr [:td "16"] [:td "4"]]
     [:tr [:td "25"] [:td "5"]]]]
   [:p "You can adjust your votes any time."]
   [:p "Group admins occasionally 'complete' a topic (ex. when a movie is watched), consuming tokens spent on that topic."]
   [:p "New tokens can be claimed by logging in once a day/week/month (depending on the group)."]

   [:p "(This is an experiment in applied " [:a {:tw "underline decoration-from-font"
                                                 :target "_blank"
                                                 :href "https://en.wikipedia.org/wiki/Quadratic_voting"} "quadratic voting"] ")"]])


(defn header-view
  [{:keys [*user]}]
  [:div.header {:tw "sticky top-0 flex justify-between items-center gap-3 bg-#3a714f text-white p-1"}
   [:div {:tw "flex items-center"}
    [:span {:tw "text-sm"} "QV"]
    [ui/icon-button {:on-click (fn []
                                 (modal/open! [info-modal-view]))}
     [fa/fa-question-circle-solid {:tw "w-3 h-3"}]]]


   (if @*user
     [:div {:tw "flex items-center gap-1"}
      [ui/icon-button {:on-click (fn []
                                   (when (js/confirm "Log out?")
                                     (-> (state/ajax!
                                          {:uri "/api/auth"
                                           :method :delete})
                                         (.then (fn []
                                                  (js/window.location.reload))))))}
       [:span {:tw "text-xs"} (:user/name @*user)]
       [fa/fa-sign-out-alt-solid {:tw "w-3 h-3"}]]]
     [:div {:tw "flex items-center gap-1"}
      [ui/icon-button {:on-click (fn []
                                   (state/auth!))}
       [:span {:tw "text-xs"} "Log In"]
       [fa/fa-sign-in-alt-solid {:tw "w-3 h-3"}]]])])
