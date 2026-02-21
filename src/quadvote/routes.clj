(ns quadvote.routes
  (:require
   [bloom.omni.auth.token :as token]
   [bloom.commons.tada.rpc.server :as tada.rpc]
   [quadvote.cqrs :as cqrs]
   [quadvote.email :as email]
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
  [[[:post "/api/auth"]
    (fn [request]
      (let [email (get-in request [:body-params :email])]
        (if-let [user-id (state/email->user-id email)]
          (do
            (future
              (let [link (wrap-login {:url "/"
                                      :user-id user-id})]
                (email/send!
                 {:to email
                  :subject "QuadVote Login Link"
                  :body [:div
                         [:p
                          [:a {:href link} "Click here"] " to log into QuadVote, or follow the link below:"]
                         [:p
                          [:a {:href link} link]]]})))
            {:status 200})
          {:status 400})))]

   [[:delete "/api/auth"]
    (fn [_]
      {:status 200
       :session nil})]

   [[:post "/api/tada/*"]
    (tada.rpc/make-handler
     cqrs/t
     {:extra-params
      (fn [request]
        {:user-id (get-in request [:session :user-id])})})]])

