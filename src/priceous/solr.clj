(ns priceous.solr
  (:require [flux.http :as http]
            [flux.core :as flux]
            [flux.query :as query]
            [taoensso.timbre :as log]))

(defn transform-to-solr-document [doc]
  (clojure.set/rename-keys
   doc
   {:image            :image_url
    :old-price        :old_price
    :link             :source_url}))

(defn query [q]
  (try 
    (flux/with-connection
      ;; TODO externalize host and collection
      (http/create "http://localhost:8983/solr" :whisky)
      (log/debug (format "SolrQuery: [%s]" q))
      (let [response
            (flux/request
             (query/create-query-request
              {:q (str "{!q.op=AND} " q) ;; change default operator to be AND
               :fq "available:true"
               :start 0 ;; TODO paging later
               :rows 50 ;; ONLY 50 RESULTS RTURNED
               :sort "price asc"}))]
        #_(log/debug (format "Found %s items" (count (:docs response))))
        {:status :success :data response}))
    (catch Exception e
      (log/error e)
      {:status :error :response {}})))


;; TODO can be avoided later
(defn transform-dashes-to-underscores [m]
  (into {} (map (fn [[k v]]
                  [(keyword (subs (clojure.string/replace (str k) "-" "_") 1)) v]
                  ) m)))

(defn write [provider items]
  (log/info "Write data to solr")
  (try
    (flux/with-connection
      ;; TODO externalize host and collection
      (http/create "http://localhost:8983/solr" :whisky)

      (log/debug "Connections to SOLR established")
      
      ;; delete all documents for this provider because currently we interested
      ;; in recent items
      (log/info provider)
      (flux/delete-by-query (str "provider_name:" (:provider-name provider)))

      ;; transform and add to solr
      (->> items
           (map transform-dashes-to-underscores)
           (flux/add))
      
      (flux/commit)
      (log/info (format "Pushed to solr %s items" (count items))))
    (catch Exception e
      (log/error "Pushing to solr failed")
      (log/error e))))
