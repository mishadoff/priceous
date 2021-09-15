(ns priceous.spider.core
  (:require [priceous.spider.solr :as solr]
            [priceous.spider.provider :as p]
            [priceous.spider.stats :as stats]
            [priceous.spider.formatter :as fmt]
            [priceous.spider.alert :as alert]
            [priceous.spider.flow :as flow]
            [priceous.system.config :as config]
            [priceous.utils.namespace :as nsutil]
            [priceous.utils.time :as time]
            [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:gen-class))

;; scan folder priceous.provider.* and include all namespaces
(nsutil/require-all-providers)

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
    (log/info "Start monitoring prices for providers" (seq provider-names))
    (let [start (System/currentTimeMillis)
          providers (->> provider-names
                         (map nsutil/resolve-provider-by-name)
                         (remove nil?))
          final-state (reduce scrap-provider {:total 0} providers)]
      (log/info (fmt/succesfully-processed-all
                  (:total final-state)
                  (time/elapsed-so-far start)))

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
          start-readable (time/now)
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
                                  pname (count items) (time/elapsed-so-far start)))
                ;; write data to every appender
                (log/info "Items: " items)

                (solr/write provider items) ;; TODO return delta

                (-> state
                    (update-in [:total] + (count items))
                    (assoc-in [(str/lower-case pname)]
                              {:provider pname
                               :count (count items)
                               :pcode-unique-count pcode-unique-count
                               :data-coverage data-coverage
                               :time-start start-readable
                               :time-end (time/now)
                               :time-taken (time/elapsed-so-far start)})))))

    (catch Exception e
      (log/error (format "[%s] ProviderError" (p/pname provider)))
      (log/error e)
      (-> state
          (assoc-in [(str/lower-case (p/pname provider))]
                    {:provider (p/pname provider) :error (.getMessage e)})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  "This main needed for on-demand scrapping without starting webserver
   lein run -m priceous.spider.core goodwine rozetka ...
   or
   lein run -m priceous.spider.core all
  "
  [& args]
  ;(config/config-timbre!)
  ;(config/read-properties!)
  ;(ssl/trust-all-certificates)
  (cond
    (= "all" (first args)) (scrap (nsutil/find-all-providers))
    :else (scrap args)))
