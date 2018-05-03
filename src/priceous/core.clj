(ns priceous.core
  (:require [priceous.solr :as solr]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.ssl :as ssl]
            [priceous.stats :as stats]
            [priceous.appender :as a]
            [priceous.formatter :as fmt]
            [priceous.config :as config]
            [priceous.alert :as alert]
            [priceous.flow :as flow]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clojure.string :as str])
  (:gen-class))

;; scan folder priceous.provider.* and include all namespaces
(u/require-all-providers)

(declare
 scrap
 scrap-provider)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn scrap
  "Scrap data for all providers sequentially.
   Input: list of provider names which will be resolved from their namespaces
          e.g. [rozetka, goodwine, winetime]"
  [provider-names]
  (try
    (log/info "Start monitoring prices for providers " (seq provider-names))
    (let [start (System/currentTimeMillis)
          providers (->> provider-names
                         (map u/resolve-provider-by-name)
                         (remove nil?))
          final-state (reduce scrap-provider {:total 0} providers)]
      (log/info (fmt/succesfully-processed-all (:total final-state) (u/elapsed-so-far start)))

      (alert/notify "Alert from priceous"
                    (str "Scrapping finished:\n\n\n"
                         (with-out-str (clojure.pprint/pprint final-state))))

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
    (let [start (System/currentTimeMillis)
          start-readable (u/now)
          items (flow/process provider)
          pname (p/pname provider)
          ;; stats
          data-coverage (stats/data-coverage-avg items)
          pcode-unique-count (count (distinct (map :product-code items)))]


      ;; TODO before/after check
      ;; - Volume range
      ;; - Alcohol range

      (cond (empty? items)
            (do (log/warn (format "[%s] No items found" pname))
                (-> state
                    (assoc-in [(str/lower-case pname)] {:provider pname :warn "No items found"})))

            :else
            (do (log/info (format "[%s] Found %s items in %.2f seconds"
                                  pname (count items) (u/elapsed-so-far start)))
                ;; write data to every appender
                (log/info (format "Available appenders %s" (config/prop [:appenders])))
                (doseq [apn (config/prop [:appenders])]
                  (a/append apn provider items)) ;; TODO return delta
                (-> state
                    (update-in [:total] + (count items))
                    (assoc-in [(str/lower-case pname)]
                              {:provider pname
                               :count (count items)
                               :pcode-unique-count pcode-unique-count
                               :data-coverage data-coverage
                               :time-start start-readable
                               :time-end (u/now)
                               :time-taken (u/elapsed-so-far start)})))))

    (catch Exception e
      (log/error (format "[%s] ProviderError" (p/pname provider)))
      (log/error e)
      (-> state
          (assoc-in [(str/lower-case (p/pname provider))]
                    {:provider (p/pname provider) :error (.getMessage e)})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  "This main needed to test scrapping without starting webserver
   lein run -m priceous.core goodwine rozetka ...
   or
   lein run -m priceous.core all
  "
  [& args]
  (config/config-timbre!)
  (config/read-properties!)
  (ssl/trust-all-certificates)
  (cond
    (= "all" (first args)) (scrap (u/find-all-providers))
    :else (scrap args)))
