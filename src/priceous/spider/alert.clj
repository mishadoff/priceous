(ns priceous.spider.alert
  (:require [priceous.system.config :as config]
            [postal.core :as mail]
            [clojure.tools.logging :as log]))

;; TODO alerting component

(defn notify [title text]
  (let []
    (mail/send-message
      {:host "smtp.gmail.com"
       :user (config/get :alert :from)
       :pass (config/get :alert :password)
       :ssl true}
      {:from (config/get :alert :from)
       :to (config/get :alert :emails)
       :subject title
       :body text})
    (log/info "Message sent!")))
