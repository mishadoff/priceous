(ns priceous.web.routes
  (:require [compojure.core :refer [GET] :as compojure]
            [compojure.route :as route]
            [priceous.web.controller :as c]
            [ring.middleware.defaults :as ring.defaults]
            [ring.middleware.ratelimit :as ring.ratelimit]
            [ring.util.response :as r]))

(defn create-routes [config]
  (compojure/routes

    (GET "/" [] (r/redirect "/search"))

    (GET "/search" request (c/search request))
    (GET "/stats" request (c/stats request))
    (GET "/help" request (c/help request))
    (GET "/about" request (c/about request))
    (GET "/contacts" request (c/contacts request))

    ;; admin
    (GET "/scrap" request (c/scrap request))

    (route/resources "/")
    (route/not-found "<h1>Invalid page</h1>")))

;;;

(defn app [config]
  (-> (create-routes config)
      (ring.defaults/wrap-defaults ring.defaults/site-defaults)
      (ring.ratelimit/wrap-ratelimit
        {:limits [(ring.ratelimit/ip-limit (-> config :server :rate-limit))]})))