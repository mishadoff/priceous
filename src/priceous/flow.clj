(ns priceous.flow
  (:require [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]
            [priceous.provider :as p]
            [priceous.formatter :as fmt]
            [net.cgrand.enlive-html :as html])
  (:import [java.util.concurrent ExecutorService Executors Callable]))

;; Important! This pool defined each time process is called
;; depending on the provider configuration

(def ^ExecutorService ^:dynamic *pool*)
(defn future* [^Callable fun] (.submit *pool* fun))
 
(declare
 process
 process-category
 process-page
 process-query-api
 fetch-heavy-nodes
 update-stats)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn process [provider]
  (binding [*pool* (Executors/newFixedThreadPool (p/threads provider))]
    (log/info (format "Created pool for %d threads." (p/threads provider)))
    (let [result (->> (p/get-categories provider)
                      (map (partial p/with-category provider))
                      (map process-category)
                      (apply concat)
                      (doall))]
      ;; we need result before shutdown the pool
      (.shutdown *pool*)
      result)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- process-category
  "Iteratively process each page for the current provider within category
   until state of the provider is not set to :done
   Function for retrieving documents is build based on provider conf"
  [provider]
  (let [p (atom provider) docs (atom [])
        process-page-fn (cond
                          (p/api? provider) process-query-api
                          (or (p/heavy? provider) (p/light? provider)) process-page
                          :else (u/die "Invalid strategy")
                          )]

    (while (not (p/done? @p))
      (let [result (process-page-fn @p)]
        (assert (:provider result) "Processor must return new provider")
        (assert (:docs result)     "Processor must return docs")
        (reset! p (:provider result))          ;; set current provider to new
        (swap! docs into (:docs result))       ;; add docs to the processed
        ))

    (log/info (fmt/category-processed @p (count @docs)))

    @docs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- process-page
  "Accept provider with configuration and retrieve page of results
  based on the current provider state
  Note: this function should return map with a new state of
  provider and retrieved docs   {:provider provider :docs []}"
  [provider]
  (p/validate-configuration provider)
  (let [fetch-page-fn (or (get-in provider [:configuration :fetch-page-fn])
                          #(u/fetch (p/current-page %)))
        page (fetch-page-fn provider)]
    (->>
     page
     ((fn [page]
        (log/info (fmt/processing-page provider (su/last-page provider page)))
        page))
     (su/find-nodes provider)
     (#(cond->> % (p/heavy? provider) (fetch-heavy-nodes provider)))
     (map (partial (p/node->document provider) provider))

     ;; here docs
     
     ((fn [docs] {:provider provider :docs (into [] (remove empty? docs))}))
     (update-stats page))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- fetch-heavy-nodes
  "Given provider with heavy strategy and list of nodes (items)
  from the results page, it process each item in a separate thread
  and load whole page as enlive node"
  [provider nodes]
  (->> nodes              
       (map (partial su/find-link provider))
       (map (fn [{link :link :as nodemap}]
              (assoc nodemap :future
                     (future* (cast Callable (fn [] (u/fetch link)))))))
       (doall) ;; <--- this is very need, so lazy, much parallelism
       (map (fn [{future :future :as nodemap}]
              (assoc nodemap :page (.get future))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- update-stats
  "Advance the provider state and checks if we are done
   based on limit, or last-page-selector"
  [page {provider :provider docs :docs :as result}]
  (assoc result :provider
         (-> provider
             (update-in [:state :page-current] inc)
             (update-in [:state :page-processed] inc)
             (update-in [:state :current-val] (get-in provider [:state :advance-fn]))
             (p/set-done-if-limit-reached)
             ((fn [p] (cond
                        (and (= :api (p/strategy p)) (empty? docs)) (p/set-done p)
                        (= :api (p/strategy p)) p
                        :else (p/set-done-if-last-page p (su/last-page p page))
                        ))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- process-query-api
  "Accept provider with configuration and retrieve results from
  api based provider using its current state
  Note: this function should return map with a new state of
  provider and retrieved docs   {:provider provider :docs []}"
  [provider]
  (p/validate-configuration provider)

  (->> provider
       ((fn [p]
          (let [data ((p/query-api-fn p) p)]
            (log/info (fmt/processing-page provider (:num-pages data)))
            data)))
       ;; result is :status :success and :docs
       ((fn [res]
          (cond
            (= :success (:status res))
            {:provider provider
             :docs (into [] (:docs res))}
            :else (u/die "Invalid query call"))))
       (update-stats nil))) ;; no page needed for api call

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

