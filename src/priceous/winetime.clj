(ns priceous.winetime
  (:require [net.cgrand.enlive-html :as html]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            ))

(defn node->link [provider node]
  (-> node
      (html/select (:selector-link provider))
      (u/ensure-one
       :required true
       :selector (:selector-link provider)
       :provider (:name provider))
      (get-in [:attrs :href])
      ((fn [part-link] (str (:base-url provider) part-link)))))

(defn node->price [provider node]
  (-> node
      (html/select (:selector-price provider))
      (u/ensure-one
       :required false
       :selector (:selector-price provider)
       :provider (:name provider))
      (get-in [:attrs :price])
      (u/safe-parse-double)
      ))

(defn node->old-price [provider node]
  (-> node
      (html/select (:selector-old-price provider))
      (u/ensure-one
       :required false
       :selector (:selector-old-price provider)
       :provider (:name provider))
      (html/text)
      (u/safe-parse-double)
      ((fn [value] (if value (/ value 100.0) nil)))
      ))

(defn node->available? [provider node]
  (-> (node->price provider node)
      (boolean)))

(defn node->sale? [provider node]
  (-> (node->old-price provider node)
      (boolean)))


(def provider
  {:name "Winetime"

   :base-url "http://winetime.com.ua/"
   :page-template "http://winetime.com.ua/ua/whiskey.htm?type_tovar=view_all_tovar&size=10000"
   :page-start 1
   :page-limit 1 ;; we process only one page with all items
   
   :selector-pages      [:.item-block_main]
   :selector-name       [:.item-block-content_main :h3 :a]
   :selector-link       [:.item-block-content_main :h3 :a]
   :selector-image      [:.item-block-head_main :a :img]
   :selector-price      [:.main_price]
   :selector-old-price  [:.old_price]

   ;; overriden methods
   :node->link node->link
   :node->price node->price
   :node->old-price node->old-price
   :node->available? node->available?
   :node->sale? node->sale?
   
   })
