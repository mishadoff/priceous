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
 create-page->docs-fn
 fetch-heavy-nodes
 update-state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn process [provider]
  (binding [*pool* (Executors/newFixedThreadPool (p/threads provider))]
    (let [result (->> (p/get-categories provider)
                      (map (partial p/with-category provider))
                      (map process-category)
                      (apply concat))]
      (.shutdown *pool*)
      result)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- process-category
  "Iteratively process each page for the current provider within category
   until state of the provider is not set to :done
   Function for retrieving documents is build based on provider conf"
  [provider]
  (let [page->docs (create-page->docs-fn provider)
        p (atom provider) docs (atom [])]
    
    (while (not (p/done? @p))
      (log/info (fmt/processing-page @p))
      (let [result (page->docs @p)]
        (assert (:provider result) "Processor must return new provider")
        (assert (:docs result)     "Processor must return docs")
        (reset! p (:provider result))          ;; set current provider to new
        (swap! docs into (:docs result))       ;; add docs to the processed
        ))

    (log/info (fmt/category-processed @p (count @docs)))    

    @docs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- create-page->docs-fn
  "Accept provider with configuration and returns function which
  can return all documents for the given provider
  Note: this function should return map with a new state of
  provider and retrieved docs   {:provider provider :docs []}"
  [provider]
  (p/validate-configuration provider)

  (fn [provider]
    (let [page (u/fetch (p/current-page provider))]
      (->>
       page
       (su/find-nodes provider)
       (#(cond->> % (p/heavy? provider) (fetch-heavy-nodes provider)))
       (map (partial (p/node->document provider) provider))
       ((fn [docs] {:provider provider :docs (into [] docs)}))
       (update-stats page))))
  )

(defn- fetch-heavy-nodes [provider nodes]
  (->> nodes              
       (map (partial su/find-link provider))
       (map (fn [{link :link :as nodemap}]
              (assoc nodemap :future
                     (future* (cast Callable (fn [] (u/fetch link)))))))
       (doall) ;; <--- this is very need, so required, much concurrency
       (map (fn [{future :future :as nodemap}]
              (assoc nodemap :page (.get future))))))

(defn- update-stats [page {provider :provider :as result}]
  (assoc result :provider
         (-> provider
             (update-in [:state :page-current] inc)
             (update-in [:state :page-processed] inc)
             (p/set-done-if-limit-reached)
             (p/set-done-if-last-page (su/last-page provider page)))))
