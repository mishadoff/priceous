(ns priceous.flow
  (:require [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [clojure.spec :as s]
            [net.cgrand.enlive-html :as html]))

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
  {:pre [(s/valid? :priceous.spec/provider provider)]}
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
               (update-in [:state :page-current] inc)
               (update-in [:state :page-processed] inc)
               (assoc-in  [:state :done] ((p/get-last-page? provider) provider page))
               (p/set-done-if-limit-reached))
           :urls urls})))))

(defn- process-heavy [provider]
  {:pre [(s/valid? :priceous.spec/provider provider)]}  
  (loop [p provider total-urls [] docs []]
    (cond
      (p/done? provider)
      (do
        (log/info (format "[%s] Processing finished. Found %s items"
                          (get-in p [:info :name])
                          (count total-urls)))
        (map deref docs))
      
      
      :else
      (do
        (log/info (format "[%s] Processing page %s"
                          (get-in p [:info :name])
                          (get-in p [:state :page-current])))
        (let [result (get-urls-for-provider p)
              new-provider (:provider result)
              new-urls (:urls result)]
          (recur new-provider
                 (into total-urls new-urls)
                 (into docs (map #(future ((get-in p [:functions :url->document]) p %)) new-urls))))))))



;; (defn get-documents-for-provider
;;   "Retrieve documents from the current state of provider

;;   STRATEGY: LIGHT
  
;;   Return modified provider (with state) and list of documents

;;   - pass returned provider to the next call to get correct state
;;   - if list is empty, stop processing

;;   {:provider provider
;;    :docs      [doc doc doc]}

;;   "
;;   [provider]
;;   {:pre [(s/valid? :priceous.spec/provider provider)]}
;;   (let [template            (get-in provider [:state :page-template])
;;         page-number         (get-in provider [:state :page-current])
;;         page-processed      (get-in provider [:state :page-processed])
;;         page-limit          (get-in provider [:state :page-limit])
;;         done                (get-in provider [:state :done])

;;         ;; functions
;;         last-page?          (or (get-in provider [:functions :last-page])
;;                                 (u/falsy))
;;         page-t
;;         ]
;;     (cond

;;       ;; indicated as done, stop processing
;;       (true? done) (empty-and-done provider)

;;       ;; we encountered a limit for processed pages
;;       (>= page-processed page-limit) (empty-and-done provider)

;;       ;; not enough information to decide stop or procees
;;       :else
;;       (let [url         (format template page-number)        
;;             page        (u/fetch url)]
;;         (cond
;;           ;; page not found
;;           (nil? page) empty-and-done

;;           ;; page exists, proceed
;;           :else
;;           ;; TODO possibly to externalize that
;;           (let [nodes (page->nodes provider page)]
;;             {:provider
;;              (-> provider
;;                  (update-in  [:state :page-current] inc)
;;                  (update-in  [:state :page-processed] inc)
;;                  (assoc-in   [:state :done] last-page?)
;;                  ((fn [p]
;;                     (let [page-processed (get-in p [:state :page-processed])
;;                           page-limit     (get-in p [:state :page-limit])]
;;                       (cond
;;                         (>= page-processed page-limit) 
;;                         (assoc-in p [:state :done] true)
;;                         :else p))))
;;                  )
;;              :docs (map #(node->document provider %) nodes)}))))))


;; (defn process-light [{:keys [page->nodes node->document] :as provider}]
;;   ;; TODO: valid light provider
  
;;   (loop [p provider docs []]
;;     (cond
;;       ;; provider processed
;;       (true? (get-in p [:state :done]))
;;       (do
;;         (log/info (format "[%s] Processing finished. Found %s items"
;;                           (get-in p [:provider-name])
;;                           (count docs)))
;;         docs)
      
;;       :else
;;       (do
;;         (log/info (format "[%s] Processing page %s"
;;                           (get-in p [:provider-name])
;;                           (get-in p [:state :page-current])))
;;         (let [result (get-documents-for-provider p)
;;               new-provider (:provider result)
;;               documents (:docs result)]
;;           (recur new-provider (into docs documents)))))))


;; register processors (ns: processor)
(defmulti process (fn [provider] (:fetch-strategy provider)))
(defmethod process :heavy [provider] (process-heavy provider))
;;(defmethod process :light [provider] (process-light provider))
(defmethod process :default [_] (u/die "Unknown :fetch-strategy"))
