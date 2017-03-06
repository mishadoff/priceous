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

            [priceous.templates.help :as help]
            [priceous.templates.stats :as stats]
            [priceous.templates.search :as search]
            [priceous.templates.contacts :as contacts]
            [priceous.templates.about :as about]

            [priceous.utils :as u]
            [priceous.ssl :as ssl]
            [priceous.scheduler :as scheduler]
            [priceous.solr :as solr]
            [priceous.core :as core])
  (:import [java.util.concurrent TimeUnit])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defroutes webapp-routes 
  (GET "/" [] (redirect "/search"))

  (GET "/search" {:keys [params] :as request}
       (-> {}
           ((fn [response]
              (cond
                ;; if query is empty just return response
                (empty? (:query params)) response

                ;; if query is present execute solr query
                :else (assoc response :response
                             (solr/query params {:ip (u/get-client-ip request)})))))
           ((fn [solr-response] {:solr solr-response :params params}))
           (search/view)))

  (GET "/stats" request (stats/view (solr/stats {:ip (u/get-client-ip request)})))
  (GET "/help" [] (help/view {}))
  (GET "/about" [] (about/view {}))
  (GET "/contacts" [] (contacts/view {}))
  
  (route/resources "/")
  (route/not-found "<h1>Invalid page</h1>") ;; TODO error page
  
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def webapp
  (-> webapp-routes
      (wrap-defaults site-defaults)
      (wrap-ratelimit {:limits [(ip-limit 1000)]}) ;; TODO extract
      wrap-keyword-params
      wrap-params
      wrap-session))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def app (routes webapp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init
  ([] (init nil))
  ([args]
   (ssl/trust-all-certificates)
   (config/config-timbre!)
   (config/read-properties! (first args))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init-scheduler []
  (log/info "SYSTEM_INIT [scheduler]")
  (scheduler/schedule-submit-function
   (fn []
     (Thread/sleep 3000)
     (log/info "Start scrapping..")
     (core/scrap (config/prop [:scrapping :providers])))
   :delay (config/prop [:scheduler :delay])
   :value (config/prop [:scheduler :value])
   :time-unit (java.util.concurrent.TimeUnit/valueOf
               (config/prop [:scheduler :time-unit]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main [& args]
  (init args)
  (log/info "Server started.")
  (init-scheduler)
  (ring/run-jetty #'app {:port (config/prop [:server :port]) :join? false}))
