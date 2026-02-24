(ns quadvote.ui.state
  (:require
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
