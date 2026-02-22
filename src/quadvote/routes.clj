(ns quadvote.routes
  (:require
   [bloom.omni.auth.token :as token]
   [bloom.commons.tada.rpc.server :as tada.rpc]
   [sys.api :as sys]
   [quadvote.cqrs :as cqrs]
   [quadvote.config :as config]
   [quadvote.email :as email]
   [quadvote.state :as state]))

(defn wrap-login
  [{:keys [url user-id]}]
  (str (config/get :website-base-url)
       url
       "?"
       (token/login-query-string
        user-id
        (config/get :omni-secret))))

(defn r
  [cqrs-registry]
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
     cqrs-registry
     ;; sys doesn't make context available via sys/get until after the system has fully started
     #_(cqrs/registry)
     {:extra-params
      (fn [request]
        {:user-id (get-in request [:session :user-id])})})]])

(def component
  {:sys.component/id :routes
   :sys.component/expects #{:tada}
   :sys.component/provides #{:routes}
   :sys.component/start (fn [{:keys [tada]}]
                          {:routes (r tada)})})

(defn routes []
  (sys/get :system :routes))
