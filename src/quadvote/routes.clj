(ns quadvote.routes
  (:require
   [clojure.string :as string]
   [bloom.commons.tada.rpc.server :as tada.rpc]
   [bloom.omni.auth.token :as token]
   [sys.api :as sys]
   [quadvote.config :as config]
   [quadvote.cqrs :as cqrs]
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

(defonce rpc-handler (delay
                       (tada.rpc/make-handler
                        (cqrs/registry)
                        {:extra-params
                         (fn [request]
                           {:user-id (get-in request [:session :user-id])})})))

;; if you change these, you have to reload omni
(def r
  [[[:post "/api/auth"]
    (fn [request]
      (let [email (-> (get-in request [:body-params :email])
                      string/trim
                      string/lower-case)
            ;; should validate more?
            path (get-in request [:body-params :path])]
        (if-let [user-id (or (state/email->user-id email)
                             (do (state/create-user! email)
                                 (state/email->user-id email)))]
          (do
            (future
              (let [link (wrap-login {:url path
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
    (fn [request]
      (@rpc-handler request))]])

(def component
  {:sys.component/id :routes
   :sys.component/expects #{:tada}
   :sys.component/provides #{:routes}
   :sys.component/start (fn [{:keys [_tada]}]
                          {:routes #'r})})

(defn routes []
  (sys/get :system :routes))
