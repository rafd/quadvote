(ns quadvote.migrate)

(require '[quadvote.state :as state])
(require '[dat.api :as dat])

(defn m2026-03-06-add-created-at-and-last-visit-at!
  []
  (let [today (java.util.Date.)]
    (dat.api/transact!
     (quadvote.state/conn)
     (concat
      (->> (dat.api/q '[:find ?topic-id
                        :where
                        [?t :topic/id ?topic-id]
                        [(missing? $ ?t :topic/created-at)]]
                      @(quadvote.state/conn))
           (map (fn [[topic-id]]
                  {:topic/id topic-id
                   :topic/created-at today})))
      (->> (dat.api/q '[:find ?membership-id
                        :where
                        [?m :membership/id ?membership-id]
                        [(missing? $ ?m :membership/last-visit-at)]]
                      @(quadvote.state/conn))
           (map (fn [[membership-id]]
                  {:membership/id membership-id
                   :membership/last-visit-at today})))))))

(defn m2026-01-30-add-new-token-fields!
  []
  (dat.api/transact!
   (quadvote.state/conn)
   (->> (dat.api/q '[:find ?membership-id
                     :where [_ :membership/id ?membership-id]]
                   @(quadvote.state/conn))
        (map (fn [[membership-id]]
               {:membership/id membership-id
                :membership/claimable-token-amount 25})))))
