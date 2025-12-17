(ns quadvote.routes
  (:require
   [bloom.omni.auth.token :as token]
   [bloom.commons.tada.rpc.server :as tada.rpc]
   [quadvote.state :as state]
   [quadvote.config :as config]))

(defn wrap-login
  [{:keys [url user-id]}]
  (str (config/get :website-base-url)
       url
       "?"
       (token/login-query-string
        user-id
        (config/get :omni-secret))))

(def routes
  [[[:get "/api/auth"]
    (fn [request]
      {:status 200
       :body {:authed? (boolean (get-in @state/state [:db/users (:user-id (:session request))]))}})]

   [[:post "/api/auth"]
    (fn [request]
      (if-let [user (state/user-by-email (get-in request [:body-params :email]))]
        (do
          (println (wrap-login {:url "/" :user-id (:user/id user)}))
          {:status 200})
        {:status 400}))]

   [[:delete "/api/auth"]
    (fn [_]
      {:status 200
       :session nil})]

   [[:post "/api/tada/*"]
    (tada.rpc/make-handler
      {:extra-params
       (fn [request]
         {:user-id (get-in request [:session :user-id])})})]])

