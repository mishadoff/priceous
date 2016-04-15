(ns priceous.core
  (:require [priceous.novus :as novus]
            [priceous.metro :as metro]
            [priceous.fozzy :as fozzy]
            [priceous.stolichnyi :as stolichnyi]
            [priceous.solr :as solr]
            [taoensso.timbre :as log]
            [clj-webdriver.taxi :as web])

  (:gen-class))

(defn monitor-provider [state name f]
  (let [items (f)] ;; process items
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
             (fn [acc {:keys [name fun]}]
               (monitor-provider acc name fun))
             ;; initial state
             {:total 0}
             providers)]

        (log/info (format "Succecfully processed %s items in %s seconds"
                          (or (:total final-state) 0)
                          (/ (- (System/currentTimeMillis) start) 1000.0)))))

    (catch Exception e
      (log/error "Processing failed " e))
    
    (finally
      (web/close))

    ))


(defn -main [& args]
  ;; process args
  (monitor-all [{:name "Novus"      :fun novus/process}
                {:name "Metro"      :fun metro/process}
                {:name "Fozzy"      :fun fozzy/process}
                {:name "Stolychnyi" :fun stolichnyi/process}]))
