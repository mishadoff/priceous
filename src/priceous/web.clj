(ns priceous.web
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.adapter.jetty :as ring]
            [ring.util.response :refer [redirect]]

            [ring.middleware.defaults
             :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params
             :refer [wrap-keyword-params]]
            
            [ring.middleware.session :refer [wrap-session]]

            [taoensso.timbre :as log]

            [priceous.config :as config]
            [priceous.template :as t]
            [priceous.scheduler :as scheduler]
            [priceous.solr :as solr]
            [priceous.core :as core])
  (:import [java.util.concurrent TimeUnit])
  (:gen-class))

(defroutes app-routes
  (GET "/" [] (redirect "/search"))
  
  (GET "/search" {params :params}
       (let [query (:query params)]
         (if (empty? query)
           (t/search-new {:title "Whisky Search"})
           (t/search-new (merge {:title "Whisky Search"
                                 :response (solr/query query)}
                                params)))))

  (GET "/admin" [] #_(t/admin))
  
  (route/resources "/")

  )

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
      wrap-keyword-params
      wrap-params
      wrap-session))

;; TODO introduce properties
(defn -main [& args]
  ;; init
  (config/config-timbre!)
  (config/read-properties! (first args))
  (log/info "Starting server...")
  
  ;; schedule data gathering every 2 hours
  (scheduler/schedule-submit-function
   (fn []
     (log/info "Start scrapping..")
     (core/gather))
   :delay (get-in @config/properties [:scheduler :delay])
   :value (get-in @config/properties [:scheduler :value])
   :time-unit (java.util.concurrent.TimeUnit/valueOf
               (get-in @config/properties [:scheduler :time-unit])))
  
  ;; start server
  (ring/run-jetty #'app
                  {:port (get-in @config/properties [:server :port])
                   :join? false}))
