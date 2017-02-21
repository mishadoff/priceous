(ns priceous.core
  (:require [priceous.solr :as solr]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.appender :as a]
            [priceous.formatter :as fmt]
            [priceous.config :as config]
            [priceous.flow :as flow]
            [clojure.java.io :as io]            
            [taoensso.timbre :as log]
            )
  (:gen-class))

;; TODO: "dependency injection"
;; scan folder priceous.provider.* and include all namespaces
(u/require-all-providers)

(declare
 scrap
 scrap-provider
 )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn scrap
  "Scrap data for all providers sequentially.
   Input: list of provider names which will be resolved from their namespaces
          e.g. [rozetka, goodwine, winetime]"
  [provider-names]
  (try
    (log/info "Start monitoring prices for providers " provider-names)
    (let [start (System/currentTimeMillis)
          providers (->> provider-names
                         (map u/resolve-provider-by-name)
                         (remove nil?))
          final-state (reduce scrap-provider {:total 0} providers)]
      (log/info (fmt/succesfully-processed-all
                 (:total final-state) (u/elapsed-so-far start)))
      final-state)
    (catch Exception e
      (log/error "Scrapping failed" e))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- scrap-provider
  "Scrap data from specific provider and return results in a state object
  Input:
   * state - previous state of scrapping, like {:total 1023}
   * provider - valid provider for scrapping
  Output:
   * state - accumulated scrapped results from current scrapping and previous state
     (Example: {:total 1455})"
  [state provider]
  (try
    (let [items (flow/process provider) pname (p/pname provider)]
      (cond (empty? items)
            (do (log/warn (format "[%s] No items found" pname)) state)

            :else
            (do (log/info (format "[%s] Found %s items" pname (count items)))
                ;; write data to every appender
                (log/info (format "Available appenders %s" (config/prop [:appenders])))
                (doseq [apn (config/prop [:appenders])]
                  (a/append apn provider items))
                (update-in state [:total] + (count items)))))
    (catch Exception e
      (log/error (format "[%s] ProviderError" (get-in provider [:info :name])))
      (log/error e)
      state)))
