(ns priceous.core
  (:require [priceous.solr :as solr]
            [priceous.utils :as u]
            [priceous.config :as config]
            [priceous.flow :as flow]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]            
            [taoensso.timbre :as log]
            )
  (:gen-class))

;; scan folder priceous.provider.* and include all namespaces
(u/require-all-providers)

(defn- scrap-provider
  "Scrap data from specific provider and return results in a state object

  Input:
   * state - previous state of scrapping, like {:total 1023}
   * provider - valid provider for scrapping

  Output:
   * state - accumulated scrapped results from current scrapping and previous state
     (Example: {:total 1455})

  "
  [state provider]
  (try
    (let [items (flow/process provider)
          pname (get-in provider [:info :name])]
      (cond
        ;; nothing found, return previous state
        (empty? items)
        (do
          (log/warn (format "[%s] No items found" pname))
          state)

        ;; data found, add to solr
        :else
        (do
          (log/info (format "[%s] Found %s items" pname (count items)))
          ;; TODO externalize APPENDER
          (solr/write provider items)
          (update-in state [:total] + (count items)))))
    (catch Exception e
      (log/error (format "[%s] ProviderError"
                         (get-in provider [:info :name])))
      (log/error e)
      state)))

;; PUBLIC

(defn scrap
  "Scrap data for all providers sequentially
  
  Input: list of provider names which will be resolved
         from their namespaces

  e.g. [rozetka, goodwine, winetime]
  
  "
  [provider-names]
  (try
    (log/info "Start monitoring prices for providers " provider-names)
    (let [start (System/currentTimeMillis)
          providers (->> (map u/resolve-provider-by-name provider-names)
                         (remove nil?))
          final-state (reduce (fn [acc provider] (scrap-provider acc provider))
                              {:total 0}
                              providers)]
      (log/info (format "Succesfully processed %s items in %s seconds"
                        (:total final-state)
                        (u/elapsed-so-far start)))
      final-state)
    (catch Exception e
      (log/error "Scrapping failed")
      (log/error e))))
