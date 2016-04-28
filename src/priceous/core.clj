(ns priceous.core
  (:require [priceous.goodwine :as gw]
            [priceous.rozetka :as rozetka]
            [priceous.metro :as metro]
            [priceous.fozzy :as fozzy]
            [priceous.novus :as novus]
            [priceous.stolichnyi :as stolichnyi]
            
            [priceous.solr :as solr]
            [priceous.utils :as u]
            [priceous.config :as config]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]            
            [priceous.flow :as flow]
            [taoensso.timbre :as log])
  (:gen-class))

;; EXTERNALIZE 
(def appenders #{})
(def ^:dynamic *csv-file* "/Users/mkoz/temp/alcohol.csv")

(defn monitor-provider
  [state {:keys [provider-name] :as provider}]
  (let [items (flow/process provider)] ;; process items
    (cond
      ;; nothing found
      (empty? items)
      (log/warn (format "[%s] No items found" provider-name)) 

      :else
      (do
        (log/info (format "[%s] Found %s items" provider-name (count items)))

        ;; sample
        ;; solr
        (solr/write provider items)
        ;; csv
        
        ;; return state as it will be caried to the 
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


(defn gather []
  (monitor-all [gw/provider]))


(defn -main [provider & args]
  (config/config-timbre!)
  (cond
    (= provider "goodwine")  (monitor-all [gw/provider])
    (= provider "rozetka")   (monitor-all [rozetka/provider])
    (= provider "metro")     (monitor-all [metro/provider])
    (= provider "novus")     (monitor-all [novus/provider])
    (= provider "fozzy")     (monitor-all [fozzy/provider])
    (= provider "stolichnyi")(monitor-all [stolichnyi/provider])

    :else (u/die "Invalid provider")))
