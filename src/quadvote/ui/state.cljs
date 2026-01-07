(ns quadvote.ui.state
  (:require
   [bloom.commons.uuid :as uuid]
   [bloom.commons.tada.rpc.client :as tada.rpc]
   [bloom.commons.ajax :as ajax]
   [bloom.omni.fx.dispatch-debounce :as debounce]
   [re-frame.core :refer [dispatch reg-sub reg-event-fx reg-fx]]
   [quadvote.model :as model]))

(defn key-by [f coll]
  (zipmap (map f coll)
          coll))

(defn ajax! [params]
  (js/Promise.
   (fn [resolve reject]
     (ajax/request
      (assoc params
             :on-success (fn [data]
                           (resolve data))
             :on-error (fn [error]
                         (reject error)))))))

(reg-fx :tada (tada.rpc/make-dispatch {:base-path "/api/tada"}))

(reg-fx :ajax ajax/request)

(reg-fx :dispatch-debounce debounce/fx)

(reg-event-fx :state/initialize!
  (fn [_ _]
    {:db {:client.db/topics {}
          :client.db/topic-voice-amounts {}
          :client.db/group-id nil
          :client.db/me nil
          :client.db/my-balance nil
          :client.db/my-votes {}
          :client.db/modal nil}
     :dispatch [::check-auth!]}))

(reg-event-fx ::check-auth!
  (fn [_ _]
    {:ajax {:uri "/api/auth"
            :method :get
            :on-success (fn [{:keys [authed?]}]
                          (when authed?
                            (dispatch [::fetch-data!])))
            :on-error (fn [])}}))

(reg-event-fx ::fetch-data!
  (fn [_ _]
    {:tada [:api/data
            {}
            {:on-success
             (fn [data]
               (dispatch [::store-data! data]))}]}))

(reg-event-fx ::store-data!
  (fn [_ [_ data]]
    {:db {:client.db/topics (key-by :topic/id (:api.data/topics data))
          :client.db/topic-voice-amounts (:api.data/topic-voice-amounts data)
          :client.db/sorted-topic-ids []
          :client.db/group-id (:api.data/group-id data)
          :client.db/me (:api.data/user data)
          :client.db/my-balance (:api.data/balance data)
          :client.db/my-votes (key-by :vote/topic-id (:api.data/votes data))}
     :dispatch [::resort!]}))

(reg-event-fx ::resort!
  (fn [{db :db} _]
    {:db (assoc db :client.db/sorted-topic-ids
           (->> db
                :client.db/topics
                vals
                shuffle ;; force randomness for equal ones
                (sort-by (fn [topic]
                           (get-in db [:client.db/topic-voice-amounts (:topic/id topic)])))
                (map :topic/id)
                reverse))}))

(reg-event-fx
 :state/logout!
 (fn [_ _]
    {:ajax {:uri "/api/auth"
            :method :delete
            :on-success (fn [_]
                          (js/window.location.reload))
            :on-error (fn [])}}))

(reg-event-fx :state/vote!
  (fn [{db :db} [_ vote topic-id previous-voice-amount new-voice-amount]]
    (let [vote (if vote
                (assoc vote :vote/voice-amount new-voice-amount)
                {:vote/id (uuid/random)
                 :vote/voice-amount new-voice-amount
                 :vote/user-id nil ;; TODO
                 :vote/topic-id topic-id})]
      {:db (-> (if (= 0 new-voice-amount)
                 (update db :client.db/my-votes dissoc topic-id)
                 (assoc-in db [:client.db/my-votes topic-id] vote))
               (update-in [:client.db/topic-voice-amounts topic-id]
                          + (- new-voice-amount
                               previous-voice-amount))
               (update :client.db/my-balance
                       - (model/token-cost previous-voice-amount new-voice-amount)))
       :dispatch-debounce [{:id ::resort
                            :dispatch [::resort!]
                            :timeout 750}]
       :tada [:api/vote!
              {:vote-id (:vote/id vote)
               :topic-id topic-id
               :group-id (:client.db/group-id db)
               :voice-amount new-voice-amount}
              {:on-success (fn [_]
                             #_(dispatch [::fetch-data!]))}]})))

(reg-event-fx :state/open-modal!
  (fn [{db :db} [_ modal-id]]
    {:db (assoc db :client.db/modal modal-id)}))

(reg-event-fx :state/create-topic!
  (fn [_ [_ text]]
    {:tada [:api/create-topic!
            {:id (uuid/random)
             :text text}
            {:on-success
             (fn [_]
               (dispatch [::fetch-data!]))}]}))

;; -------

(reg-sub
 :state/user
 (fn [db]
   (:client.db/me db)))

(reg-sub :state/modal
  (fn [db _]
    (:client.db/modal db)))

(reg-sub :state/ranked-topics
  (fn [db _]
    (->> (:client.db/sorted-topic-ids db)
         (map (fn [topic-id]
                (get-in db [:client.db/topics topic-id]))))
    #_(->> (:client.db/topics db)
         vals
         (sort-by (fn [topic]
                    (get-in db [:client])
                    ))
         #_(sort-by (fn [topic]
                    (get-in db [:client.db/topic-voice-amounts (:topic/id topic)])))
         reverse)))

(reg-sub :state/topic-voice-amount
  (fn [db [_ topic-id]]
    (get-in db [:client.db/topic-voice-amounts topic-id] 0)))

(reg-sub :state/my-balance
  (fn [db _]
    (:client.db/my-balance db)))

(reg-sub :state/vote-for-topic
  (fn [db [_ topic-id]]
    (get-in db [:client.db/my-votes topic-id])))


