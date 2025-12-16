(ns quadvote.routes
  (:require
    [bloom.commons.tada.rpc.server :as tada.rpc]
    [quadvote.state :as state]))

(def routes
  [[[:get "/api/auth"]
    (fn [request]
      ;; TODO actually auth
      (if (get-in @state/state [:db/users (:user-id (:session request))])
        {:status 200}
        {:status 200
         :session {:user-id (key (first (:db/users @state/state)))}}))]

   [[:post "/api/tada/*"]
    (tada.rpc/make-handler
      {:extra-params
       (fn [request]
         {:user-id (get-in request [:session :user-id])})})]])

