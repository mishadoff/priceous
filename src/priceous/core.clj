(ns priceous.core
  (:require [priceous.solr :as solr]
            [priceous.utils :as u]
            [priceous.config :as config]
            [priceous.flow :as flow]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]            
            [taoensso.timbre :as log]
            
            ;; providers
            [priceous.goodwine :as gw]
            [priceous.rozetka :as rozetka]
            [priceous.fozzy :as fozzy]
            )
  (:gen-class))

(defn- monitor-provider [state provider]
  (let [items (flow/process provider)
        provider-name (get-in provider [:info :name])]
    (cond
      ;; nothing found
      (empty? items)
      (log/warn (format "[%s] No items found" provider-name)) 

      :else
      (do
        (log/info (format "[%s] Found %s items" provider-name (count items)))

        ;; solr appender
        (solr/write provider items)        

        ;; return state as it will be caried to the next
        (update-in state [:total] + (count items))))))

(defn monitor-all [providers]
  (try
    (let [start (System/currentTimeMillis)]
      (log/info "Start monitoring prices")
      
      ;; process each provider
      (let [final-state
            (reduce
             ;; sequentially monitor each provider
             (fn [acc provider]
               (monitor-provider acc provider))
             ;; initial state
             {:total 0}
             providers)]
        
        (log/info (format "Succecfully processed %s items in %s seconds"
                          (or (:total final-state) 0)
                          (/ (- (System/currentTimeMillis) start) 1000.0)))))
    
    (catch Exception e
      (log/error "Processing failed")
      (log/error e))
    
    ))

(def provider-map
  {
   "goodwine"       gw/provider  
   "rozetka"        rozetka/provider
   "fozzy"          fozzy/provider
   ;; "metro"          metro/provider
   ;; "novus"          novus/provider
   ;; "stolichnyi"     stolichnyi/provider
   ;; "winetime"       winetime/provider
   
   })

(defn gather [provider-names]
  (log/debug provider-names)
  (monitor-all (map #(get provider-map %) provider-names)))
