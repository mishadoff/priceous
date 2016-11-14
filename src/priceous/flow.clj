(ns priceous.flow
  (:require [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [net.cgrand.enlive-html :as html])
  (:import [java.util.concurrent ExecutorService Executors Callable]))

(def ^ExecutorService pool (Executors/newFixedThreadPool 16))
(defn future-in-the-pool [^ExecutorService pool ^Callable fun]
  (.submit pool fun))


(defn- empty-and-done
  "Set :done state for the provider and returns with empty url-list"
  [provider]
  {:provider (p/set-done provider) :urls []})


(defn- get-urls-for-provider
  "Retrieve urls from the current state of provider
  Used if :fetch-strategy = :heavy
  Return modified provider (with state) and list of urls
  
  - pass returned provider to the next call to get correct state
  - if list is empty, stop processing
  
  {:provider provider
   :urls      [url url url]}

  "
  [provider]
  (cond
    ;; indicated as done, stop processing
    
    (p/done? provider) (empty-and-done provider)
    
    ;; we encountered a limit for processed pages
    (p/limit-reached? provider) (empty-and-done provider)
    
    ;; not enough information to decide stop or procees
    :else
    (let [url         (format (p/get-page-template provider)
                              (p/get-page-current provider))
          page        (u/fetch url)]

      (cond
        ;; page not found
        (nil? page) (empty-and-done provider)
        
        ;; page exists, proceed
        :else
        (let [urls ((p/get-page->urls provider) provider page)]
          {:provider
           (-> provider
               ((fn [p]
                  (assoc-in p [:state :done] (not ((p/get-last-page? p) p page)))))
               (update-in [:state :page-current] inc)
               (update-in [:state :page-processed] inc)
               (p/set-done-if-limit-reached))
           :urls urls})))))

(defn- process-heavy [provider]
  (loop [p provider total-urls [] docs []]
      (cond
        (p/done? p)
        (do
          (if (:category p)
            (log/info (format "[%s | %s] Processing finished. Found %s items"
                              (get-in p [:info :name])
                              (get-in p [:state :category])
                              (count total-urls)))
            (log/info (format "[%s] Processing finished. Found %s items"
                              (get-in p [:info :name])
                              (count total-urls))))
          (->> (map #(.get %) docs)
               (remove nil?)))
        
        
        :else
        (do
          (if (:category p)
            (log/info (format "[%s | %s] Processing page %s"
                              (get-in p [:info :name])
                              (get-in p [:state :category])
                              (get-in p [:state :page-current])))
            (log/info (format "[%s] Processing page %s"
                              (get-in p [:info :name])
                              (get-in p [:state :page-current]))))
          (let [result (get-urls-for-provider p)
                new-provider (:provider result)
                new-urls (:urls result)]
            (recur new-provider
                   (into total-urls new-urls)
                   (into docs
                         (map #(future-in-the-pool
                                pool
                                (cast Callable (fn [] ((get-in p [:functions :url->document]) p %)))) new-urls))))))))



(defn get-documents-for-provider
  "Retrieve documents from the current state of provider

  STRATEGY: LIGHT
  
  Return modified provider (with state) and list of documents

  - pass returned provider to the next call to get correct state
  - if list is empty, stop processing

  {:provider provider
   :docs      [doc doc doc]}

  "
  [provider]
  (let [template            (get-in provider [:state :page-template])
        page-number         (get-in provider [:state :page-current])
        page-processed      (get-in provider [:state :page-processed])
        page-limit          (get-in provider [:state :page-limit])
        done                (get-in provider [:state :done])
        last-page           (get-in provider [:functions :last-page])
        page->nodes         (get-in provider [:functions :page->nodes])
        node->document      (get-in provider [:functions :node->document])]
    (cond

      ;; indicated as done, stop processing
      (true? done) (empty-and-done provider)

      ;; we encountered a limit for processed pages
      (>= page-processed page-limit) (empty-and-done provider)

      ;; not enough information to decide stop or procees
      :else
      (let [url         (format template page-number)        
            page        (u/fetch url)]
        (cond
          ;; page not found
          (nil? page) (empty-and-done provider)

          ;; page exists, proceed
          :else
          ;; TODO possibly to externalize that
          (let [nodes (page->nodes provider page)]
            {:provider
             (-> provider
                 (update-in  [:state :page-current] inc)
                 (update-in  [:state :page-processed] inc)
                 ((fn [p]
                    (let [page-processed (get-in p [:state :page-processed])
                          page-current   (get-in p [:state :page-current])
                          page-limit     (get-in p [:state :page-limit])]
                      (cond
                        (or (>= page-processed page-limit)
                            (> page-current (last-page provider page))) 
                        (assoc-in p [:state :done] true)
                        :else p))))
                 )
             :docs (map #(node->document provider %) nodes)}))))))


(defn process-light [provider]
  ;; TODO: valid light provider
  (loop [p provider docs []]
    (cond
      ;; provider processed
      (true? (get-in p [:state :done]))
      (do
        (log/info (format "[%s] Processing finished. Found %s items"
                          (get-in p [:info :name])
                          (count docs)))
        docs)
      
      :else
      (do
        (log/info (format "[%s] Processing page %s"
                          (get-in p [:info :name])
                          (get-in p [:state :page-current])))
        (let [result (get-documents-for-provider p)
              new-provider (:provider result)
              documents (:docs result)]
          (recur new-provider (into docs documents)))))))

(defn get-docs-from-api-provider
  "Retrieve documents from the current state of provider

  STRATEGY: API
  
  Return modified provider (with state) and list of documents

  - pass returned provider to the next call to get correct state
  - if list is empty, stop processing

  {:provider provider
   :docs      [doc doc doc]}

  "
  [provider]
  (let [template            (get-in provider [:state :api-uri])
        page-number         (get-in provider [:state :page-current])
        page-processed      (get-in provider [:state :page-processed])
        page-limit          (get-in provider [:state :page-limit])
        done                (get-in provider [:state :done])
        get-docs-fn         (get-in provider [:functions :docs])]
    (cond

      ;; indicated as done, stop processing
      (true? done) (empty-and-done provider)

      ;; we encountered a limit for processed pages
      (>= page-processed page-limit) (empty-and-done provider)

      ;; not enough information to decide stop or procees
      :else
      (let [{:keys [status docs] :as result} (get-docs-fn provider)]
        (cond
          (= :error status) (empty-and-done provider)

          :else
          (let []
            {:provider
             (-> provider
                 (update-in  [:state :page-current] inc)
                 (update-in  [:state :page-processed] inc)
                 ((fn [p]
                    (let [page-processed (get-in p [:state :page-processed])
                          page-current   (get-in p [:state :page-current])
                          page-limit     (get-in p [:state :page-limit])]
                      (cond
                        (or (>= page-processed page-limit)
                            (zero? (count docs))) 
                        (assoc-in p [:state :done] true)
                        :else p)))))
             :docs docs}))))))


(defn process-api [provider]
  ;; TODO: valid light provider
  (loop [p provider docs []]
    (cond
      ;; provider processed
      (true? (get-in p [:state :done]))
      (do
        (log/info (format "[%s] Processing finished. Found %s items"
                          (get-in p [:info :name])
                          (count docs)))
        docs)
      
      :else
      (do
        (log/info (format "[%s] Processing page %s"
                          (get-in p [:info :name])
                          (get-in p [:state :page-current])))
        (let [result (get-docs-from-api-provider p)
              new-provider (:provider result)
              documents (:docs result)]
          (recur new-provider (into docs documents)))))))

;; register processors (ns: processor)
(defmulti process (fn [provider] (:fetch-strategy provider)))
(defmethod process :heavy [provider] (process-heavy provider))
(defmethod process :light [provider] (process-light provider))
(defmethod process :api   [provider] (process-api   provider))
(defmethod process :default [_] (u/die "Unknown :fetch-strategy"))
