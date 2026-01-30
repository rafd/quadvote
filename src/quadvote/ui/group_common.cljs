(ns quadvote.ui.group-common
  (:require
   [bloom.commons.pages :as pages]
   [bloom.commons.fontawesome :as fa]
   [quadvote.ui.state :as state]
   [quadvote.ui.common :as ui]
   [quadvote.ui.modal :as modal]))

(defn header-view
  [membership]
  [:div.header {:tw "flex justify-between items-center pb-4 gap-3"}
   [:h1 {:tw "grow"}
    (:group/name (:membership/group @membership))]
   #_[ui/button {:on-click (fn [] (modal/open! :modal/new-topic))}
      [fa/fa-plus-circle-solid {:tw "w-4 h-4"}]
      "Suggest a Topic"]
   (let [amount (:membership/claimable-token-amount @membership)]
     (when (< 0 (or amount 0))
       [:div
        [:span {:tw "text-xs"}
         "Claim your bonus → "]
        [:button {:on-click (fn []
                              (-> (state/tada!
                                   [:api/claim!
                                    {:membership-id (:membership/id @membership)}])
                                  (.then (fn [_]
                                           (state/refresh! membership)))))}
         [ui/token-amount-view amount :gain]]]))
   [ui/button {:on-click (fn []
                           (pages/navigate-to! [:page/group {:id (:group/id (:membership/group @membership))}]))}
    [:span {:tw "text-xs"} "Active"]]
   [ui/button {:on-click (fn []
                           (pages/navigate-to! [:page/log {:id (:group/id (:membership/group @membership))}]))}
    [:span {:tw "text-xs"} "Complete"]]
   [ui/button {:on-click (fn [] (modal/open! :modal/info))}
    [fa/fa-question-circle-solid {:tw "w-3 h-3"}]
    [:span {:tw "text-xs"} "WTF?"]]
   [:div.my-balance {:tw "flex items-center gap-1"}
    [ui/token-amount-view (:membership/balance @membership) nil]]
   [ui/button {:on-click (fn []
                           (state/ajax!
                            {:uri "/api/auth"
                             :method :delete
                             :on-success (fn [_]
                                           (js/window.location.reload))
                             :on-error (fn [])}))}
    [fa/fa-sign-out-alt-solid {:tw "w-3 h-3"}]]])

(defn page
  [{:keys [membership]} content]
  [:div {:tw "px-8 p-4 max-w-40rem mx-auto"}
   [modal/modal-view]
   [header-view membership]
   content])
