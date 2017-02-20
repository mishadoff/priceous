(ns priceous.web
  (:require [compojure.core :refer [defroutes routes context GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.adapter.jetty :as ring]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.json
             :refer [wrap-json-response wrap-json-body]]

            [ring.middleware.defaults
             :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params
             :refer [wrap-keyword-params]]
            
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.ratelimit :refer [wrap-ratelimit ip-limit]]
            
            [taoensso.timbre :as log]

            [priceous.config :as config]
            [priceous.template :as t]
            [priceous.utils :as u]
            [priceous.scheduler :as scheduler]
            [priceous.solr :as solr]
            [priceous.core :as core])
  (:import [java.util.concurrent TimeUnit])
  (:gen-class))

(defroutes webapp-routes 
  
   (GET "/" [] (redirect "/search"))

   (GET "/search" {:keys [params] :as request}
        (let [query (:query params) advanced (:advanced params)]
          (if (empty? query)
            (t/search-new {:title "Whisky Search"})
            (t/search-new (merge {:title "Whisky Search"
                                  :response (solr/query query {:ip (u/get-client-ip request)})}
                                 params)))))

   (GET "/stats" request (t/stats {:title "Whisky Search :: Stats"
                                   :response (solr/stats {:ip (u/get-client-ip request)} )}))
   (GET "/help" [] (t/help {:title "Whisky Search :: Help"}))
   
   (route/resources "/")
   (route/not-found "<h1>Invalid page</h1>")
   
   )

(def webapp
  (-> webapp-routes
      (wrap-defaults site-defaults)
      (wrap-ratelimit {:limits [(ip-limit 1000)]}) ;; TODO extract
      wrap-keyword-params
      wrap-params
      wrap-session))

(def app (routes webapp api))

(defn init
  ([] (init nil))
  ([args]
   (config/config-timbre!)
   (config/read-properties! (first args))))

;; TODO introduce properties
(defn -main [& args]
  (init args)
  (log/info "Server started.")
  
  ;; schedule data gathering every 2 hours
  (scheduler/schedule-submit-function
   (fn []
     ;; sleep 3 seconds just because
     (Thread/sleep 3000)
     (log/info "Start scrapping..")
     (core/scrap
      (get-in @config/properties [:scrapping :providers])))
   :delay (get-in @config/properties [:scheduler :delay])
   :value (get-in @config/properties [:scheduler :value])
   :time-unit (java.util.concurrent.TimeUnit/valueOf
               (get-in @config/properties [:scheduler :time-unit])))
  
  ;; start server
  (ring/run-jetty #'app
                  {:port (get-in @config/properties [:server :port])
                   :join? false}))
