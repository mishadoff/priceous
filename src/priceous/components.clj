(ns priceous.components
  (:require [integrant.core :as ig]
            [priceous.web.routes :as web]
            [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as log]))

;;; Components declaration

(def declaration
  {:system/config {}

   :server/jetty  {:config (ig/ref :system/config)
                   :webapp (ig/ref :server/webapp)}

   :server/webapp {:config (ig/ref :system/config)}
   })

;; System config

(defmethod ig/init-key :system/config [_ opts]
  (log/info "Init configuration")
  {:server {:port 8080}
   :name "App"
   :ratelimit 1000}
  ;; TODO schema validation
  )

(defmethod ig/halt-key! :system/config [_ _])

;;; Jetty server

(defmethod ig/init-key :server/jetty [_ {:keys [config webapp]}]
  (log/info "Init jetty server")
  (jetty/run-jetty webapp {:port (-> config :server :port) :join? false}))

(defmethod ig/halt-key! :server/jetty [_ server]
  (.stop server))

;;; Webapp

(defmethod ig/init-key :server/webapp [_ {:keys [config]}]
  (log/info "Init webapp")
  (web/app config))

(defmethod ig/halt-key! :server/webapp [_ _])

;;;