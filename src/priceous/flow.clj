(ns priceous.flow
  (:require [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]
            [priceous.provider :as p]
            [net.cgrand.enlive-html :as html])
  (:import [java.util.concurrent ExecutorService Executors Callable]))

(def ^ExecutorService pool (Executors/newFixedThreadPool 10))
(defn future-in-the-pool [^ExecutorService pool ^Callable fun]
  (.submit pool fun))

(declare
 process
 process-for-category
 create-page->docs-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn process [provider]
  (->> ((p/category-function provider) provider)         ;; retrieve categories
       (map (partial p/provider-with-category provider)) ;; modify provider
       (map process-for-category)                        ;; process for category
       (apply concat)))                                  ;; merge results

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO how to rewrite it in more idiomatic way?
;; TODO timeout for category?
(defn- process-for-category [provider]
  (let [page->docs (create-page->docs-fn (:configuration provider))]
    (loop [p provider docs []]
      (cond
        (p/done? p)
        (do
          (log/info (format "[%s | %s] Category processed. Found %s items"
                            (get-in p [:info :name])
                            (get-in p [:state :category])
                            (count docs)))
          docs)

        ;; not finished processing
        :else
        (do
          (log/info (format "[%s | %s] Processing page %s"
                            (get-in p [:info :name])
                            (get-in p [:state :category])
                            (get-in p [:state :page-current])))
          ;; TODO validate function return results
          (let [result (page->docs p)]
            (recur (:provider result) (into docs (:docs result)))))))))



(defn- create-page->docs-fn
  "Accept configuration and returns function which
  can return all documents for the given provider
  Note: this function should return map with a new state of
  provider and retrieved docs   {:provider provider :docs []}
  "
  [conf]
  (let [known-strategies   #{:heavy :light :api}
        parallel-count     (get-in conf [:parallel-count] 1)
        strategy           (get-in conf [:strategy])
        url-selector       (get-in conf [:url-selector])
        url-selector-type  (get-in conf [:url-selector-type])
        node-selector      (get-in conf [:node-selector])
        node->document     (get-in conf [:node->document])
        last-page-selector (get-in conf [:last-page-selector])]
    
    ;; CONFIG VALIDATION
    
    (assert (known-strategies strategy)
            (str "Strategy must be one of " known-strategies))
    (assert node->document "node->document must be provided")
    
    ;; validate heavy
    (when (= strategy :heavy)
      (log/debug "> Heavy strategy")
      (assert url-selector "URL Selector must be provided")
      )

    ;; validate light
    (when (= strategy :light)
      (log/debug "> Light strategy")
      (assert node-selector "Node selector must be provided")
      )

    ;; RETURN FUNCTION
    
    (fn [provider]
      ;; fetch the page
      (let [page (u/fetch (p/current-page provider))]
        (->> page

             ;; get all urls/nodes for current page
             ((fn [page]
                (condp = strategy
                  :heavy ((su/generic-page-urls url-selector) provider page)
                  :light (su/select-mul-req page provider node-selector)
                  :api (u/die "Not implemented")
                  (u/die "Invalid strategy")
                  )))

             ;; if urls partial make full href using base-url
             ((fn [urls-or-nodes]
                (if (and (= strategy :heavy)
                         (= url-selector-type :relative-to-base))
                  (->> urls-or-nodes
                       (map (fn [u] (u/full-href provider u))))
                  urls-or-nodes)))

             
             ;; fetch all pages by url OR do nothing (nodes with context)
             ((fn [urls-or-nodes]
                (condp = strategy
                  :heavy (->> urls-or-nodes
                              ;; fetch all urls in parallel
                              (map (fn [url] [(future-in-the-pool pool (cast Callable (fn [] (u/fetch url)))) {:url url}]))
                              (map (fn [[fut ctx]] [(.get fut) ctx])))
                  ;; else add empty context
                  (->> urls-or-nodes (map (fn [node] [node {}]))))))
             
             ;; Here is the thing:
             ;; at this point we have either list of nodes with ctx
             ;; each of node/page represents one document
             ;; good thing, we can process node/page with the same api
             ;; TODO: maybe we need to process documents in parallel as well?
             
             (map (fn [[node ctx]] (node->document provider node ctx)))
             
             ((fn [docs] {:provider provider :docs (into [] docs)}))
         
             ;; update provider stats
             (#(update-in % [:provider :state :page-current] inc))
             (#(update-in % [:provider :state :page-processed] inc))
             
             ;; if limit reached set done
             (#(if (>= (get-in % [:provider :state :page-processed])
                       (get-in % [:provider :state :page-limit]))
                 (assoc-in % [:provider :state :done] true)
                 %))
             
             ;; if current page > last page
             ((fn [result]
                (let [last-page-num
                      (if (= last-page-selector :one-page) 1
                          ((su/create-last-page-fn last-page-selector)
                           (:provider result) page))]
                  (if (> (get-in result [:provider :state :page-current]) last-page-num)
                    (assoc-in result [:provider :state :done] true)
                    result))))


             )))))
