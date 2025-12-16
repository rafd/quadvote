(ns quadvote.model)

(def max-voice-amount-per-vote 5)

(defn token-cost
  [previous-voice-amount new-voice-amount]
  (- (* new-voice-amount new-voice-amount)
     (* previous-voice-amount previous-voice-amount)))

(defn can-afford?
  [balance previous-voice-amount new-voice-amount]
  (<= 0 (- balance (token-cost previous-voice-amount new-voice-amount))))
