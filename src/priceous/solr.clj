(ns priceous.solr
  (:require [flux.http :as http]
            [flux.core :as flux]
            [flux.query :as query]
            [taoensso.timbre :as log]
            [clj-time.coerce :as tc]
            [priceous.config :as config]
            [priceous.utils :as u])
  (:import [org.apache.solr.client.solrj.util ClientUtils]))

;; TODO test
(defn query [q ctx]
  (try 
    (flux/with-connection
      (http/create
       (get-in @config/properties [:solr :host])
       (keyword (get-in @config/properties [:solr :collection])))
      (let [processed-query (if (clojure.string/starts-with? q "!")
                              (subs q 1)
                              (ClientUtils/escapeQueryChars q))
            response
            (flux/request
             (query/create-query-request
              {:q processed-query
               :fq "available:true" ;; TODO remove if available true
               :start 0 ;; TODO paging later
               :rows 50 ;; TODO ONLY 50 RESULTS RTURNED
               :sort "price asc"}))]
        (log/info (format "[%s] Completed SolrQuery [%s] found %s items" (:ip ctx) q
                           (get-in response [:response :numFound])))
        {:status :success :data response}))
    (catch Exception e
      (log/error e)
      {:status :error :response {}})))

(defn- process-provider-pivots [response]
  (let [pivot (first (vals (get-in response [:facet_counts :facet_pivot])))]
    (mapv (fn [pp] {:name (:value pp)
                    :total (:count pp)
                    :available (->> (:pivot pp)
                                    (filter #(= (:value %) true))
                                    (first)
                                    (:count))}) pivot)))

(defn stats [ctx]
  (try 
    (flux/with-connection
      (http/create
       (get-in @config/properties [:solr :host])
       (keyword (get-in @config/properties [:solr :collection])))
      (log/info (format "[%s] Requested StatsRequest" (:ip ctx)))
      (let [response
            (flux/request 
             (query/create-query-request
              {:q "*"
               :start 0 
               :rows 0
               :json.facet.providers
               "{type:terms,
                 field:provider,
                 facet:{
                   ts:\"max(timestamp)\",
                   available:{
                     type:terms,
                     field:available}}}"
               }))]
        {:status :success
         :response {:total (get-in response [:response :numFound])
                    :providers (->> (get-in response [:facets :providers :buckets])
                                    (mapv (fn [bkt]
                                            {:name (:val bkt)
                                             :total (:count bkt)
                                             :ts (-> (:ts bkt) long u/to-date)
                                             :available (or (some->> (get-in bkt [:available :buckets])
                                                                     (filter :val)
                                                                     (first)
                                                                     (:count))
                                                            0)})))}}))
    (catch Exception e
      (log/error e)
      {:status :error :response {}}))
  )

(defn transform-dashes-to-underscores [m]
  (into {} (map (fn [[k v]]
                  [(keyword (subs (clojure.string/replace (str k) "-" "_") 1))
                   v]) m)))


(defn write [provider items]
  (try
    (flux/with-connection
      (http/create
       (get-in @config/properties [:solr :host])
       (keyword (get-in @config/properties [:solr :collection])))
      
      (log/info "Connections to SOLR established")
      
      ;; delete all documents for this provider because currently we interested
      ;; in recent items
      (log/info provider)
      (flux/delete-by-query (str "provider:" (get-in provider [:info :name])))
      
      ;; transform and add to solr
      (->> items
           (map transform-dashes-to-underscores)
           (flux/add))
      
      (flux/commit)
      (log/info (format "Pushed to solr %s items" (count items))))
    (catch Exception e
      (log/error "Pushing to solr failed")
      (log/error e))))
