(ns quadvote.model)

(def max-voice-amount-per-vote 5)

(defn token-cost
  [old-voice-amount new-voice-amount]
  (- (* new-voice-amount new-voice-amount)
     (* old-voice-amount old-voice-amount)))

(defn can-afford?
  [{:keys [balance old-voice-amount new-voice-amount]}]
  (<= 0 (- balance (token-cost old-voice-amount new-voice-amount))))
