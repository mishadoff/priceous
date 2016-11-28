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
    (let [items

          ;; TODO this logic should be moved to flow ns
          (cond
            (:category provider)
            (let [cat-fn (get-in provider [:functions :categories])
                  cats (cat-fn provider)]
              (->> (map (fn [[cat-name cat-url]]
                          (-> provider
                              (assoc-in [:state :page-template] cat-url)
                              (assoc-in [:state :category] cat-name))) cats)
                   (map flow/process)
                   (apply concat)))
            
            :else (flow/process provider))

          provider-name (get-in provider [:info :name])]
      (cond
        ;; nothing found, return previous state
        (empty? items)
        (do
          (log/warn (format "[%s] No items found" provider-name))
          state)

        :else
        (do
          (log/info (format "[%s] Found %s items" provider-name (count items)))

          ;; TODO externalize APPENDER
          (solr/write provider items)

          (update-in state [:total] + (count items)))))
    (catch Exception e
      (log/error e)
      state)))


;; PUBLIC

(defn scrap
  "Scrap data for all providers sequentially

  Input: list of provider names which will be resolved
         from their namespaces
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
