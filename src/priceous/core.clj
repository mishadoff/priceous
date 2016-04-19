(ns priceous.core
  (:require [priceous.novus :as novus]
            [priceous.metro :as metro]
            [priceous.fozzy :as fozzy]
            [priceous.stolichnyi :as stolichnyi]
            [priceous.rozetka :as rozetka]

            [priceous.solr :as solr]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            
            [priceous.config :as config]
            [priceous.flow :as flow]
            [taoensso.timbre :as log]
            [clj-webdriver.taxi :as web])

  (:gen-class))

;; EXTERNALIZE 
(def appenders #{:csv :solr})
(def ^:dynamic *csv-file* "/Users/mkoz/temp/alcohol.csv")

(defn monitor-provider [state name flow]
  (let [items (flow/process flow)] ;; process items
    (cond
      ;; nothing found
      (empty? items)
      (log/warn (format "[%s] No items found" name)) 

      :else
      (do
        (log/info (format "[%s] Found %s items" name (count items)))

        (when (:csv appenders)
          ;; write header
          (log/info (str "Writing to file " *csv-file*))
          (with-open [out-file (io/writer *csv-file* :append false)]
            (csv/write-csv
             out-file
             [["Name" "Price" "Image" "Source" "Sale" "Old Price"]]))
          
          (with-open [out-file (io/writer *csv-file* :append true)]
            ;; write data
            (csv/write-csv
             out-file
             (->> items
                  (mapv ;; transform document map into vector
                   (fn [{:keys [name price image source sale old-price]}]
                     [name price image source sale old-price])))))
          )
          
          

        (when (:solr appenders) 
          ;; writing to solr
          (solr/write
           {:timestamp (System/currentTimeMillis) :provider name}
           items))
        
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
  (monitor-all [#_{:name "Novus"      :flow   novus/flow}
                #_{:name "Metro"      :flow   metro/flow}
                #_{:name "Fozzy"      :flow   fozzy/flow}
                #_{:name "Stolychnyi" :flow   stolichnyi/flow}
                {:name "Rozetka"      :flow   rozetka/flow}

                ]))
