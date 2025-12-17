(ns quadvote.state
  (:require
   [bloom.commons.uuid :as uuid]
   [quadvote.model :as model]
   [taoensso.nippy :as nippy]
   [quadvote.config :as config]
   [duratom.core :as d]))

;; topic
;;  topic/id
;;  topic/text

;; vote
;;   vote/id
;;   vote/topic-id
;;   vote/user-id
;;   vote/voice-amount

;; user
;;  user/id
;;  user/name
;;  user/email

;; :db/topics  {:topic/id topic}
;; :db/users   {:user/id user}
;; :db/balances {:user/id balance}
;; :db/votes {:vote/id vote}

(def initial-state
  {:db/topics {}
   :db/users {}
   :db/balances {}
   :db/votes {}})

(defonce state (d/duratom :local-file
                          :file-path (config/get :db-file-path)
                          :init initial-state
                          :rw {:read nippy/thaw-from-file
                               :write nippy/freeze-to-file}))

(defn votes->topic-voice-amounts
  [votes]
  (->> votes
       (group-by :vote/topic-id)
       (map (fn [[topic-id votes]]
              [topic-id (reduce + (map :vote/voice-amount votes))]))
       (into {})))

(defn user-can-afford?
  [{:keys [user-id topic-id vote-id voice-amount]}]
  (let [vote (get-in @state [:db/votes vote-id])
        topic (get-in @state [:db/topics topic-id])
        balance (get-in @state [:db/balances user-id])]
    (model/can-afford? balance
                       (or (:vote/voice-amount vote) 0)
                       voice-amount)))

(defn user-has-no-other-vote-for-topic?
  [user-id vote-id topic-id]
  (empty?
    (->> (:db/votes @state)
         vals
         (filter (fn [vote]
                   (and
                     (= user-id (:vote/user-id vote))
                     (= topic-id (:vote/topic-id vote)))))
         (remove (fn [vote]
                   (= vote-id (:vote/id vote)))))))

(defn user-with-id-exists?
  [user-id]
  (boolean (get-in @state [:db/users user-id])))

(defn user-by-email
  [email]
  (->> (:db/users @state)
       (some (fn [[_ user]]
               (when (= email (:user/email user))
                 user)))))

(defn user-is-admin?
  [user-id]
  (get-in @state [:db/users user-id :user/admin?]))

(defn topic-with-id-exists?
  [topic-id]
  (boolean (get-in @state [:db/topics topic-id])))

;; actions

(defn create-user!
  [{:keys [name email admin?]}]
  (let [id (uuid/random)]
    (swap! state assoc-in [:db/users id]
           {:user/id id
            :user/name name
            :user/email email
            :user/admin? admin?})))

(defn create-topic!
  [text]
  (let [id (uuid/random)]
    (swap! state assoc-in [:db/topics id]
           {:topic/id id
            :topic/text text})))

(defn vote!
  [vote-id topic-id user-id voice-amount]
  (if (= 0 voice-amount)
    (swap! state update :db/votes dissoc vote-id)
    (swap! state assoc-in [:db/votes vote-id]
           {:vote/id vote-id
            :vote/topic-id topic-id
            :vote/user-id user-id
            :vote/voice-amount voice-amount})))

(defn claim-token!
  [user-id]
  (swap! state update-in [:db/balances user-id] inc 50))
