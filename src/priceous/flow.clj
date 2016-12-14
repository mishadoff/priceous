(ns priceous.flow
  (:require [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [net.cgrand.enlive-html :as html]))

(declare
 ;; PUBLIC
 process
 
 ;; PRIVATE
 process-for-category
 )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn process [provider]
  (log/info (get-in provider [:functions :categories] (fn [p] [["Без категории" (get-in p [:state :page-template])]])))
  (let [cat-fn (get-in provider [:functions :categories]
                       (fn [p] [["Без категории" (get-in p [:state :page-template])]]))
        cats (cat-fn provider)]
    (->> (map (fn [[cat-name cat-url]]
                (-> provider
                    (assoc-in [:state :page-template] cat-url)
                    (assoc-in [:state :category] cat-name))) cats)
         (map process-for-category)
         (apply concat))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- process-for-category [provider]
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
        (let [result ((get-in p [:functions :page->docs]) p)]
          (recur (:provider result) (into docs (:docs result))))))))
