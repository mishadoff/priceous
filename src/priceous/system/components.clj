(ns priceous.system.components
  (:require [integrant.core :as ig]
            [priceous.system.init :as init]
            [priceous.system.config :as config]
            [priceous.web.routes :as web]
            [ragtime.core :as ragtime]
            [ragtime.jdbc :as ragtime.jdbc]
            [priceous.db.postgres :as pg]
            [hikari-cp.core :as hikari]
            [ring.adapter.jetty :as jetty]
            [clojure.tools.logging :as log])
  (:import (org.apache.solr.client.solrj.impl HttpSolrClient$Builder)))

;;; Components declaration

(def declaration
  {:system/init   {}
   :system/config {:init (ig/ref :system/init)}

   :db/postgres {:config (ig/ref :system/config)}

   :db/migrations {:config (ig/ref :system/config)
                   :db (ig/ref :db/postgres)}

   :server/jetty  {:config (ig/ref :system/config)
                   :webapp (ig/ref :server/webapp)}

   :server/webapp {:config (ig/ref :system/config)}

   :solr/client {:config (ig/ref :system/config)}

   })

;;;

(defmethod ig/init-key :system/init [_ _]
  (log/info "Preinit system")
  (init/trust-all-certificates!)
  :ok)

;; System config

(defmethod ig/init-key :system/config [_ opts]
  (log/info "Init configuration")
  (config/read-config!))

(defmethod ig/halt-key! :system/config [_ _])

;; Database connection

(defmethod ig/init-key :db/postgres [_ {:keys [config]}]
  (log/info "Init postgres connection pool")
  (pg/register-postgres-type-bindings)
  (let [datasource-options (:postgres config)]
    {:datasource (hikari/make-datasource datasource-options)}))

(defmethod ig/halt-key! :db/postgres [_ datasource]
  (log/info "Closing postgres connection pool")
  (-> datasource
      :datasource
      (hikari/close-datasource)))

;; Migrations

(defmethod ig/init-key :db/migrations [_ {:keys [config db]}]
  (log/info "Performing migrations")
  (let [migrations (ragtime.jdbc/load-resources "migrations")
        datastore (ragtime.jdbc/sql-database db {:migrations-table "migrations"})]
    (ragtime/migrate-all
      datastore
      {}
      migrations
      {:reporter (fn [_ op id]
                   (case op
                     :up   (log/info "Applying: " id)
                     :down (log/info "Rolling back: " id)))})))

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
