(ns quadvote.migrate
  (:require
   [taoensso.nippy :as nippy]
   [duratom.core :as d]
   [quadvote.state :as state]
   [dat.api :as dat]))

(comment
  (def state (d/duratom :local-file
                        :file-path "state.nippy"
                        :init {:db/topics {}
                               :db/users {}
                               :db/balances {}
                               :db/votes {}}
                        :rw {:read nippy/thaw-from-file
                             :write nippy/freeze-to-file}))

  (deref state)

  (def group-id (dat/uuid))
  (def raf-user-id (->> (vals (:db/users @state))
                        (filter (fn [{:user/keys [email]}]
                                  (= email "rafal.dittwald@gmail.com")))
                        first
                        :user/id))

  (dat/transact!
   state/conn
   [{:user/id raf-user-id
     :user/name "Raf"
     :user/email "rafal.dittwald@gmail.com"}])

  (dat/transact!
   state/conn
   [{:db/id -1
     :group/id group-id
     :group/name name}
    {:membership/id (dat/uuid)
     :membership/user [:user/id raf-user-id]
     :membership/group -1
     :membership/admin? true}])

  (dat/transact!
   state/conn
   (concat (->> (vals (:db/topics @state))
               (map (fn [{:topic/keys [id title description]}]
                      {:topic/id id
                       :topic/title title
                       :topic/description description
                       :topic/group [:group/id group-id]
                       :topic/user [:user/id raf-user-id]})))

          (->> (vals (:db/users @state))
               (mapcat (fn [{:user/keys [id name email admin?]}]
                         [{:user/id id
                           :user/name name
                           :user/email email}
                          {:membership/id (dat/uuid)
                           :membership/user [:user/id id]
                           :membership/group [:group/id group-id]
                           :membership/balance (get-in @state [:db/balances id] 0)
                           :membership/admin? (boolean admin?)}])))

          (->> (vals (:db/votes @state))
               (map (fn [{:vote/keys [id topic-id user-id voice-amount]}]
                      {:vote/id id
                       :vote/voice-amount voice-amount
                       :vote/topic [:topic/id topic-id]
                       :vote/user [:user/id user-id]}))))))
