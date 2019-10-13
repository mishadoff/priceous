(ns priceous.solr.client
  (:require [priceous.system.state :as system]
            [flux.core :as flux]
            [flux.query :as query]))

;;;

(defn query [request]
  (let [connection (-> system/system :solr/client)]
    (assert (some? connection))
    (flux/with-connection connection
      (flux/request (query/create-query-request request)))))

;;;