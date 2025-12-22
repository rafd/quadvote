(ns quadvote.email
  (:require
   [bloom.commons.html :as html]
   [postal.core :as postal]
   [quadvote.config :as config]))

(defn send!
  [email-or-emails]
  (doseq [email (if (map? email-or-emails)
                  [email-or-emails]
                  email-or-emails)]
    (if-let [email-creds (config/get :smtp-credentials)]
      (postal/send-message
       email-creds
       {:to (email :to)
        :from (email-creds :from)
        :subject (email :subject)
        :body [{:type "text/html; charset=utf-8"
                :content (html/render (email :body))}]})
      (println "Sending email:" email))))
