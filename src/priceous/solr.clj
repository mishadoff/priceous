(ns priceous.solr
  (:require [flux.http :as http]
            [flux.core :as flux]
            [taoensso.timbre :as log]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]

            ))

(defn- transform-to-solr-document
  [{:keys [timestamp provider] :as common-data}
   {:keys [name image source price sale old-price] :as item}]
  (-> {:name name
       :image_url image
       :source_url source
       :price price
       :sale sale
       :old_price old-price
       :provider provider
       :timestamp (tf/unparse (tf/formatters :date-time-no-ms) (tc/from-long timestamp))
       }))

(defn write [{:keys [provider] :as common-data}
             items]
  (log/info "Write data to solr")
  (try
    (flux/with-connection
      ;; TODO externalize host and collection
      (http/create "http://localhost:8983/solr" :whisky)

      ;; delete all documents for this provider because currently we interested
      ;; in recent items
      (flux/delete-by-query (str "provider:" provider))

      ;; transform and add to solr
      (-> (map #(transform-to-solr-document common-data %) items) (flux/add))
      
      (flux/commit)
      (log/info (format "Pushed to solr %s items" (count items))))
    (catch Exception e
      (log/error "Pushing to solr failed")
      (log/error e))))
