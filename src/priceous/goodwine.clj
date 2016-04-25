(ns priceous.goodwine
  (:require [net.cgrand.enlive-html :as html]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            ))

(def provider
  {:name "Goodwine"

   :page-template "http://goodwine.com.ua/whisky/c4502/page=%s/"
   :page-start 1
;;   :page-limit 1
   
   :selector-pages      [:.gtile-item]
   :selector-name       [:.g-title-bold :a]
   :selector-link       [:.g-title-bold :a]
   :selector-price      [:.price.price-retail]
   :selector-image      [:.gtile-i-img :a :img]
   :selector-available? [:.g-status-available]
   :selector-sale?      [:.price-label-bottles-remains]

   ;; overriden methods
   :node->old-price (fn [_ _] nil)
   
   })
