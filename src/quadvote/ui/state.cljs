(ns quadvote.ui.state
  (:require
   [clojure.string :as string]
   [bloom.commons.ajax :as ajax]
   [bloom.commons.uuid :as uuid]
   [reagent.core :as r]
   [quadvote.model :as model]))

(defn ajax! [params]
  (js/Promise.
   (fn [resolve reject]
     (ajax/request
      (assoc params
             :on-success (fn [data]
                           (resolve data))
             :on-error (fn [error]
                         (reject error)))))))

(defn tada!
  [[event-id event-params]]
  (js/Promise.
   (fn [resolve reject]
     (ajax/request
      {:uri (str "/api/tada/"
                 (when (namespace event-id)
                   (str (namespace event-id) "."))
                 (name event-id))
       :method :POST
       :params {:event-id event-id
                :event-params event-params}
       :on-success resolve
       :on-error reject}))))

(defonce tada-atoms-cache (atom {}))

(defn tada-atom!
  [e]
  (if-let [a (get @tada-atoms-cache e)]
    a
    (let [refresh-fn (atom nil)
          a (let [a (with-meta (r/atom nil)
                      {::refresh-fn refresh-fn
                       ::error (r/atom nil)})]
              (swap! tada-atoms-cache assoc e a)
              a)
          f (fn []
              (-> (tada! e)
                  (.then (fn [v]
                           (reset! a v)))
                  (.catch (fn [err]
                            (swap! tada-atoms-cache update e vary-meta update ::error reset! err)))))]
      (reset! refresh-fn f)
      (f)
      a)))

(defn refresh!
  [a]
  (@(::refresh-fn (meta a))))

(defn error
  [a]
  @(::error (meta a)))

; ---

(defonce group-id (r/atom nil))

; --

(defn auth!
  []
  (let [email (js/prompt "Enter your email to receive a login link:")]
    (if (and email
             (re-matches #"^[^\s@]+@[^\s@]+\.[^\s@]+$" email))
      (-> (ajax!
           {:uri "/api/auth"
            :method :post
            :params {:email email
                     :path js/window.location.pathname}})
          (.then (fn []
                   (js/alert "Email sent. Check your inbox.")))
          (.catch (fn [e]
                    (js/console.error "Auth error:" e)
                    (js/alert "Something went wrong. Try again?"))))
      (js/alert "Email address invalid. Please try again."))))

(defn require-auth [f]
  (if @(tada-atom! [:api/user {}])
    (f)
    (when (js/confirm "You need to be logged in to do that. Log in now?")
      (auth!))))

; ---

(defn topic->total-voice-amount
  [topic]
  (->> topic
       :vote/_topic
       (map :vote/voice-amount)
       (reduce +)))

(defn topic->user-vote
  [topic user-id]
  (->> topic
       :vote/_topic
       (filter (fn [vote]
                 (= user-id (:user/id (:vote/user vote)))))
       first))

