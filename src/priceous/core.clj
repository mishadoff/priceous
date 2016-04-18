(ns priceous.core
  (:require [priceous.novus :as novus]
            [priceous.metro :as metro]
            [priceous.fozzy :as fozzy]
            [priceous.stolichnyi :as stolichnyi]
            [priceous.solr :as solr]
            [priceous.config :as config]
            [priceous.flow :as flow]
            [taoensso.timbre :as log]
            [clj-webdriver.taxi :as web])

  (:gen-class))

(defn monitor-provider [state name flow]
  (let [items (flow/process flow)] ;; process items
    (cond
      ;; nothing found
      (empty? items)
      (log/warn (format "[%s] No items found" name)) 

      :else
      (do
        (log/info (format "[%s] Found %s items" name (count items)))
        (solr/write
         {:timestamp (System/currentTimeMillis) :provider name}
         items)
        ;; return state as it will be caried to the 
        (update-in state [:total] + (count items))))))

(defn monitor-all [providers]
  (try
    (let [start (System/currentTimeMillis)]
      (log/info "Init Selenium Driver")
      (web/set-driver! {:browser :firefox})

      (log/info "Start monitoring prices")

      ;; process each provider
      (let [final-state
            (reduce
             ;; sequentially monitor each provider
             (fn [acc {:keys [name flow]}]
               (monitor-provider acc name flow))
             ;; initial state
             {:total 0}
             providers)]

        (log/info (format "Succecfully processed %s items in %s seconds"
                          (or (:total final-state) 0)
                          (/ (- (System/currentTimeMillis) start) 1000.0)))))

    (catch Exception e
      (log/error "Processing failed")
      (log/error e))
    
    (finally
      (web/close))

    ))


(defn -main [& args]
  (config/config-timbre!)
  ;; process args
  (monitor-all [{:name "Novus"      :flow   novus/flow}
                {:name "Metro"      :flow   metro/flow}
                {:name "Fozzy"      :flow   fozzy/flow}
                {:name "Stolychnyi" :flow   stolichnyi/flow}]))
