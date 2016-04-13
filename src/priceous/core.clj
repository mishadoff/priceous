(ns priceous.core
  (:require [priceous.novus :as novus]
            [priceous.metro :as metro]
            [priceous.fozzy :as fozzy]
            [priceous.stolichnyi :as stolichnyi]
            [priceous.solr :as solr]
            [clj-webdriver.taxi :as web]))

(defn monitor-flow [state name f]
  (let [items (f)]
    (println (format "Found %s items on %s" (count items) name))

    (swap! state + (count items))

    (println "Pushing data to SOLR...")
    (solr/write items)
    (println "Pushing data to SOLR Done.")
    
    state
    ))

(defn monitor-all []
  (let [start (System/currentTimeMillis)
        state (atom 0)]
    (println "Init Selenium Driver...")
    (web/set-driver! {:browser :firefox})

    (println "Start monitoring prices...")
    (-> state ;; inititial counter
        (monitor-flow "Novus" novus/whiskey-prices)
        (monitor-flow "Metro" metro/whiskey-prices)
        (monitor-flow "Fozzy" fozzy/whiskey-prices)
        (monitor-flow "Stolichnyi" stolichnyi/whiskey-prices)
        ;; TODO rozetka
        ;; TODO goodwine

        )
    (web/close)

    
    (println (format "Processed %s items" @state))
    (println (format "Processing done in %s seconds"
                     (/ (- (System/currentTimeMillis) start) 1000.0)))
    ))
