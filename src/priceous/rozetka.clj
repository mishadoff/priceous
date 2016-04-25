(ns priceous.rozetka
  (:require [taoensso.timbre :as log]
            [net.cgrand.enlive-html :as html]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            ))

(defn extract-rozetka-price [s [a b]]
  (->> s
       ;; remove encoded symbols like %22, %3A
       ((fn [s] (clojure.string/replace s #"%.." "")))
       (re-seq (re-pattern (str a "([0-9\\.]+)" b)))
       (first)
       (second)
       (u/safe-parse-double)))


(defn node->price [provider node]
  (-> node
      (html/select [[:div (html/attr= :name "prices_active_element_original")] :script])
      (u/ensure-one
       :required false
       :selector "prices_active_element_original"
       :provider (:name provider))
      (html/text)
      (extract-rozetka-price ["price" "price_formatted"])
      ))

(defn node->old-price [provider node]
  (-> node
      (html/select [[:div (html/attr= :name "prices_active_element_original")] :script])
      (u/ensure-one
       :required false
       :selector "prices_active_element_original"
       :provider (:name provider))
      (html/text)
      (extract-rozetka-price ["old_price" "old_price_formatted"])
      ))

(defn node->available? [provider node]
  (-> node
      (html/select [:.g-i-status-unavailable])
      (u/ensure-one
       :required false
       :selector [:.g-i-status-unavailable]
       :provider (:name provider))
      (boolean)
      (not)
      ))

(defn node->image [provider node]
  (-> node
      (html/select (:selector-image provider))
      (u/ensure-one
       :required false
       :selector (:selector-image provider)
       :provider (:name provider))
      (get-in [:attrs :data_src])
      ))

(def provider
  {:name "Rozetka"
   :page-template "http://rozetka.com.ua/krepkie-napitki/c4594292/filter/page=%s;vid-napitka-69821=whiskey-blend,whiskey-bourbon,whiskey-single-malt/"
   :page-start 1
;;   :page-limit 1
   :log-item? true

   :selector-pages [:.g-i-tile-i-box-desc]
   :selector-name  [:.g-i-tile-i-title :a]
   :selector-link  [:.g-i-tile-i-title :a]
   :selector-image [:.g-i-tile-i-image :a :img]
   :selector-sale? [:.g-i-tile-i-promotions-title]
   
   :node->image node->image
   :node->price node->price
   :node->old-price node->old-price
   :node->available? node->available?
   
   })
