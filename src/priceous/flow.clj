(ns priceous.flow
  (:require [clj-webdriver.taxi :as web]
            [clj-webdriver.core :as c]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.api :as api]
            )
  (:import [org.openqa.selenium NoSuchElementException]))

;;;;;;;;;;;;; API


(defprotocol IFlow
  "Defines a flow for scrapping"
  
  (select-all-items-from-page [this])
  (select-name-from-item [this item])
  (select-link-from-item [this item])
  (select-image-from-item [this item])
  (select-price-from-item [this item])
  (select-old-price-from-item [this item])
  (select-sale-from-item [this item])
  (valid-element? [this item])

  (page-template [this])
  (context [this])
  
  )


;;;;;;;;;;;;;



(defn- process-page [flow url]
  (web/to url)
  (try
    (loop [[item & items] (select-all-items-from-page flow) returned []]
      (cond
        ;; all items are processed or nothing found
        (nil? item) returned

        :else 
        (cond
          ;; name not found means no more products
          (not (valid-element? flow item)) returned

          :else 
          (let [name (select-name-from-item flow item)
                original-link (select-link-from-item flow item)
                product-image (select-image-from-item flow item)
                price (select-price-from-item flow item)
                sale (select-sale-from-item flow item)
                old-price (select-old-price-from-item flow item)
                item {:name name
                      :image product-image
                      :source original-link
                      :price price
                      :sale sale
                      :old-price old-price}]
            ;; logging only name do not pollute namespace
            (log/info (:name item))
            (recur items (conj returned item))))))
    (catch Exception e
      (log/error (format "Error processing items [%s]" e)))))

(defn process
  "Process iteratively all pages by provided page-template starting from 1"
  [flow]
  (loop [page 1 items []]
    (log/info (format "Crawling page %s" page))
    (let [url (format (page-template flow) page)
          page-items (process-page flow url)]
      (if (empty? page-items)
        items
        (do
          (log/info (format "Crawled %s items" (count page-items)))
          (recur (inc page) (into items page-items)))))))