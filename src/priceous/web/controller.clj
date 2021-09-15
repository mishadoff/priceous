(ns priceous.web.controller
  (:require [priceous.utils.http :as utils.http]
            [priceous.spider.solr :as solr]
            [priceous.spider.core :as spider]
            [priceous.web.templates.search :as search]
            [priceous.web.templates.stats :as stats]
            [priceous.web.templates.help :as help]
            [priceous.web.templates.about :as about]
            [priceous.web.templates.contacts :as contacts]))

;; TODO rewrite
(defn search [{:keys [params] :as request}]
  (-> {}
      ((fn [response]
         (cond
           ;; if query is empty just return response
           (empty? (:query params)) response

           ;; if query is present execute solr query
           :else (assoc response :response
                                 (solr/query params {:ip (utils.http/get-client-ip request)})))))
      ((fn [solr-response] {:solr solr-response :params params}))
      (search/view)))


;;;

(defn stats [request]
  (stats/view
    (solr/stats {:ip (utils.http/get-client-ip request)})))

;;;

(defn help [request]
  (help/view {}))

;;;

(defn about [request]
  (about/view {}))

;;;

(defn contacts [request]
  (contacts/view {}))

;;;

(defn scrap [request]
  (future
    (spider/scrap ["goodwine"]))
  {:status 200 :body "ok"})