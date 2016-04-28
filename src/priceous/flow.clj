(ns priceous.flow
  (:require [taoensso.timbre :as log]
            [priceous.utils :as u]
            [net.cgrand.enlive-html :as html]))

(defn get-urls-for-provider
  "Retrieve urls from the current state of provider

  STRATEGY: HEAVY
  
  Return modified provider (with state) and list of urls

  - pass returned provider to the next call to get correct state
  - if list is empty, stop processing

  {:provider provider
   :urls      [url url url]}

  "
  [{:keys [page->urls last-page] :as provider}]
  
  (let [empty-and-done      (-> {:provider provider :urls []}
                                (assoc-in [:provider :state :done] true))
        template            (get-in provider [:state :page-template])
        page-number         (get-in provider [:state :page-current])
        page-processed      (get-in provider [:state :page-processed])
        page-limit          (get-in provider [:state :page-limit])
        done                (get-in provider [:state :done])]

    (cond

      ;; indicated as done, stop processing
      (true? done) empty-and-done

      ;; we encountered a limit for processed pages
      (>= page-processed page-limit) empty-and-done

      ;; not enough information to decide stop or procees
      :else
      (let [url         (format template page-number)        
            page        (u/fetch url)
            ;; if last page function is defined use it
            last-page?  (if last-page
                          (= (last-page provider page) page-number) false)]
        (cond

          ;; page not found
          (nil? page) empty-and-done

          ;; page exists, proceed
          :else
          ;; TODO possibly to externalize that
          (let [urls (page->urls provider page)]
            {:provider
             (-> provider
                 (update-in  [:state :page-current] inc)
                 (update-in  [:state :page-processed] inc)
                 (assoc-in   [:state :done] last-page?)
                 ((fn [p]
                    (let [page-processed (get-in p [:state :page-processed])
                          page-limit     (get-in p [:state :page-limit])]
                      (cond
                        (>= page-processed page-limit) 
                        (assoc-in p [:state :done] true)
                        :else p))))
                 )
             :urls urls}))))))

(defn process-heavy [{:keys [url->document page->urls] :as provider}]
  {:pre [url->document page->urls]}
  (loop [p provider total-urls [] docs []]
    (cond
      ;; provider processed
      (true? (get-in p [:state :done]))
      (do
        (log/info (format "[%s] Processing finished. Found %s items"
                      (get-in p [:provider-name])
                      (count total-urls)))
        (map deref docs))
      
      
      :else
      (do
        (log/info (format "[%s] Processing page %s"
                      (get-in p [:provider-name])
                      (get-in p [:state :page-current])))
        (let [result (get-urls-for-provider p)
              new-provider (:provider result) new-urls (:urls result)]
          (recur new-provider (into total-urls new-urls)
                 (into docs (map #(future (url->document p %)) new-urls))
                 ))))))



(defn get-documents-for-provider
  "Retrieve documents from the current state of provider

  STRATEGY: LIGHT
  
  Return modified provider (with state) and list of documents

  - pass returned provider to the next call to get correct state
  - if list is empty, stop processing

  {:provider provider
   :docs      [doc doc doc]}

  "
  [{:keys [page->nodes node->document last-page] :as provider}]
  
  (let [empty-and-done      (-> {:provider provider :docs []}
                                (assoc-in [:provider :state :done] true))
        template            (get-in provider [:state :page-template])
        page-number         (get-in provider [:state :page-current])
        page-processed      (get-in provider [:state :page-processed])
        page-limit          (get-in provider [:state :page-limit])
        done                (get-in provider [:state :done])]

    (cond

      ;; indicated as done, stop processing
      (true? done) empty-and-done

      ;; we encountered a limit for processed pages
      (>= page-processed page-limit) empty-and-done

      ;; not enough information to decide stop or procees
      :else
      (let [url         (format template page-number)        
            page        (u/fetch url)
            ;; if last page function is defined use it
            last-page?  (if last-page
                          (= (last-page provider page) page-number) false)]
        (cond

          ;; page not found
          (nil? page) empty-and-done

          ;; page exists, proceed
          :else
          ;; TODO possibly to externalize that
          (let [nodes (page->nodes provider page)]
            {:provider
             (-> provider
                 (update-in  [:state :page-current] inc)
                 (update-in  [:state :page-processed] inc)
                 (assoc-in   [:state :done] last-page?)
                 ((fn [p]
                    (let [page-processed (get-in p [:state :page-processed])
                          page-limit     (get-in p [:state :page-limit])]
                      (cond
                        (>= page-processed page-limit) 
                        (assoc-in p [:state :done] true)
                        :else p))))
                 )
             :docs (map #(node->document provider %) nodes)}))))))


(defn process-light [{:keys [page->nodes node->document] :as provider}]
  {:pre [page->nodes node->document]}
  (loop [p provider docs []]
    (cond
      ;; provider processed
      (true? (get-in p [:state :done]))
      (do
        (log/info (format "[%s] Processing finished. Found %s items"
                          (get-in p [:provider-name])
                          (count docs)))
        docs)
      
      :else
      (do
        (log/info (format "[%s] Processing page %s"
                          (get-in p [:provider-name])
                          (get-in p [:state :page-current])))
        (let [result (get-documents-for-provider p)
              new-provider (:provider result)
              documents (:docs result)]
          (recur new-provider (into docs documents)))))))

;; TODO dispatch
(defn process [{:keys [strategy] :as provider}]
  (cond
    (= strategy :heavy) (process-heavy provider)
    (= strategy :light) (process-light provider)
    :else (u/die "Unknown :strategy provider")))
