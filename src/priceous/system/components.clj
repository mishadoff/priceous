(ns priceous.system.components
  (:require [integrant.core :as ig]
            [priceous.system.init :as init]
            [priceous.system.config :as config]
            [priceous.web.routes :as web]
            [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as log])
  (:import (org.apache.solr.client.solrj.impl HttpSolrClient$Builder)))

;;; Components declaration

(def declaration
  {:system/init   {}
   :system/config {:init (ig/ref :system/init)}

   :server/jetty  {:config (ig/ref :system/config)
                   :webapp (ig/ref :server/webapp)}

   :server/webapp {:config (ig/ref :system/config)}

   :solr/client {:config (ig/ref :system/config)}

   })

;;;

(defmethod ig/init-key :system/init [_ _]
  (log/info "Preinit system")
  (init/config-timbre!)
  (init/trust-all-certificates!)
  :ok)

;; System config

(defmethod ig/init-key :system/config [_ opts]
  (log/info "Init configuration")
  (config/read-config!))

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

;;; Solr Client

(defmethod ig/init-key :solr/client [_ {:keys [config]}]
  (let [connection-string (str (-> config :solr :host)
                               "/"
                               (-> config :solr :collection))]
    (log/info "Init solr connection:" connection-string)
    (-> (HttpSolrClient$Builder. connection-string)
        (.build))))

(defmethod ig/halt-key! :solr/client [_ _])
