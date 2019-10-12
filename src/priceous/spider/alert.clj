(ns priceous.spider.alert
  (:require [priceous.config :as config]
            [postal.core :as mail]
            [taoensso.timbre :as log]))

(defn notify [title text]
  (let [emails (config/prop [:alert :emails])]
    (mail/send-message
      {:host "smtp.gmail.com"
       :user (config/prop [:alert :from])
       :pass (config/prop [:alert :password])
       :ssl true}
      {:from (config/prop [:alert :from])
       :to (config/prop [:alert :emails])
       :subject title
       :body text})
    (log/info "Message sent!")))
