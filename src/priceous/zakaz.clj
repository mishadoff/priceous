(ns priceous.zakaz
  (:require [clj-webdriver.taxi :as web]
            [clj-webdriver.core :as c]
            [priceous.utils :as u]
            ))

;; TODO logger
;; TODO change selenium to rest with masks
;; TODO make null safe hierarchical queries
;; TODO error safe

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generic Package to Process zakaz.ua specific items ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- process-page [url]
  (web/to url)
  (try
    (loop [[item & items] (web/find-elements {:class "one-product"}) returned []]
      (cond
        ;; all items are processed
        (nil? item) returned

        :else 
        (do 
          (let [name (clojure.string/trim (web/text (u/find item "one-product-name")))]
            (cond
              ;; name not found means no more products
              (empty? name) returned

              :else 
              (let [product-link-e (u/find item "one-product-link")
                    original-link (c/attribute product-link-e "href")
                    product-image-e (u/find product-link-e "one-product-image")
                    image (web/attribute (web/find-element-under product-image-e {:tag "img"}) "src")
                    product-button-e (u/find item "one-product-button")
                    one-product-price-e (u/find product-button-e "one-product-price")
                    price-grn (c/text (u/find one-product-price-e "grivna price"))
                    price-cop (c/text (u/find one-product-price-e "kopeiki"))]
                (recur items (conj returned {:name name
                                             :image image
                                             :source original-link
                                             :price (Double/parseDouble (str price-grn "." price-cop))}))))))

        ))
    (catch Exception e
      (println (format "Error processing items [%s]" (.getMessage e))))))


(defn process [base-url]
  (let [base-url base-url]
    (loop [page 1 items []]
      (println (format "Processing page %s..." page))
      (let [url (format "%s/?&page=%s" base-url page)
            page-items (process-page url)]
        (if (empty? page-items)
          items
          (do
            (println (format "Processed %s items" (count page-items)))
            (recur (inc page) (into items page-items))))))))
