(ns priceous.provider-helper
  (:require [priceous.selector-utils :as su]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [taoensso.timbre :as log]
            [net.cgrand.enlive-html :as html]
            )
  (:import [java.util.concurrent ExecutorService Executors Callable]))




(defn create-page->docs-fn
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
        (->> 
         page

         ;; get all urls/nodes for current page
         (fn [page]
           (condp = strategy
             :heavy ((su/generic-page-urls url-selector) provider page)
             :light (u/die "Not implemented")
             :api (u/die "Not implemented")
             (u/die "Invalid strategy")
             ))

         ;; fetch all pages by url OR do nothing
         (fn [urls-or-nodes]
           (condp = strategy
             :heavy (-> urls-or-nodes
                        ;; fetch all urls in parallel
                        (map #(future-in-the-pool pool (cast Callable (fn [] (u/fetch %)))))
                        (map #(.get %)))
             urls-or-nodes))
         
         ;; Here is the thing:
         ;; at this point we have either list of nodes OR list of pages
         ;; each of node/page represents one document
         ;; good thing, we can process node/page with the same api
         ;; TODO: maybe we need to process documents in parallel as well?

         (map (partial node->document provider))
         
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
         (#(if (> (get-in % [:provider :state :page-current])
                  ((create-last-page-fn last-page-selector) % page))
             (assoc-in % [:provider :state :done] true)
             %))         
         )))))

