(ns priceous.solr
  (:require [flux.http :as http]
            [flux.core :as flux]))

(defn write [items]
  (flux/with-connection (http/create "http://localhost:8983/solr" :whiskey)
    (->> items
         (map (fn [{:keys [name image source price]}]
                (-> {:name_s name
                     :image_s image
                     :source_s source
                     :price_f price}
                    (assoc :ts (System/currentTimeMillis)))))
         (flux/add))
    (flux/commit)))
