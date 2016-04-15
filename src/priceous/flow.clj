(ns priceous.flow
  (:require [clj-webdriver.taxi :as web]
            [clj-webdriver.core :as c]
            [taoensso.timbre :as log]
            [priceous.utils :as u])
  (:import [org.openqa.selenium NoSuchElementException]))

(defn select-all-items-from-page [{:keys [provider] :as context}]
  (u/selenium-failsafe-apply
   context
   #(web/find-elements {:class "one-product"})
   "class: one-product"))

(defn- select-name-from-item [{:keys [provider] :as context} item]
  (u/selenium-failsafe-apply
   context
   #(->> (web/find-element-under item {:class "one-product-name"})
         (web/text)
         (clojure.string/trim))
   "class: one-product-name > text"))

(defn- select-link-from-item [{:keys [provider] :as context} item]
  (u/selenium-failsafe-apply
   context
   #(-> (web/find-element-under item {:class "one-product-link"})
        (c/attribute "href"))
   "class: one-product-link > attr: href"))

(defn- select-image-from-item [{:keys [provider] :as context} item]
  (u/selenium-failsafe-apply
   context
   #(-> item
        (web/find-element-under {:class "one-product-link"})
        (web/find-element-under {:class "one-product-image"})
        (web/find-element-under {:tag "img"})
        (web/attribute "src"))
   "class: one-product-link > class: one-product-image > tag: img > attr: src"))

;; FIXME zakaz specific
(defn- price-from-element [e]
  (Double/parseDouble
   (str (c/text (web/find-element-under e {:class "grivna price"}))
        "."
        (c/text (web/find-element-under e {:class "kopeiki"})))))

(defn- select-price-from-item [{:keys [provider] :as context} item]
  (u/selenium-failsafe-apply
   context
   #(-> item
        (web/find-element-under {:class "one-product-button"})
        (web/find-element-under {:class "one-product-price"})
        (price-from-element))
   "class: one-product-button > class: one-product-price > class: grivna price & kopeiki"))

(defn- select-old-price-from-item [{:keys [provider] :as context} item]
  (u/selenium-failsafe-apply
   context
   #(-> item
        (web/find-element-under {:class "badge right-up-sale-bage"})
        (price-from-element))
   "class: badge right-up-sale-bage > class: grivna price & kopeiki"))

(defn- select-sale-from-item [{:keys [provider] :as context} item]
  (u/selenium-failsafe-apply
   context
   #(-> item
        (web/find-element-under {:class "badge right-up-sale-bage"})
        (boolean))
   "class: badge right-up-sale-bage"))

(defn- valid-element? [context item]
  (not (empty? (select-name-from-item context item))))

(defn- process-page [context url]
  (web/to url)
  (try
    (loop [[item & items] (select-all-items-from-page context) returned []]
      (cond
        ;; all items are processed or nothing found
        (nil? item) returned

        :else 
        (cond
          ;; name not found means no more products
          (not (valid-element? context item)) returned

          :else 
          (let [name (select-name-from-item context item)
                original-link (select-link-from-item context item)
                product-image (select-image-from-item context item)
                price (select-price-from-item context item)
                sale (select-sale-from-item context item)
                old-price (select-old-price-from-item context item)
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
  [context page-template]
  (loop [page 1 items []]
    (log/info (format "Crawling page %s" page))
    (let [url (format page-template page)
          page-items (process-page context url)]
      (if (empty? page-items)
        items
        (do
          (log/info (format "Crawled %s items" (count page-items)))
          (recur (inc page) (into items page-items)))))))
