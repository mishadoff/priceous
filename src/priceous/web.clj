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
            
            [priceous.config :as config]
            [priceous.template :as t]
            [priceous.solr :as solr]
            [priceous.core :as core]))

(defroutes app-routes
  (GET "/" [] (redirect "/search"))
  
  (GET "/search" {params :params}
       (let [query (:query params)]
         (if (empty? query)
           (t/search {:title "Whisky Search"})
           (t/search {:title "Whisky Search"
                      :query query
                      :items (solr/query query)}))))

  (GET "/admin" [] "TBD")
  (POST "/admin/gather" []
        (future (core/gather))
        :ok)
  
  (route/resources "/")

  )

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
      wrap-keyword-params
      wrap-params
      wrap-session))

(defn -main []
  ;; TODO port from property
  (config/config-timbre!)
  (ring/run-jetty #'app {:port 3000 :join? false}))
