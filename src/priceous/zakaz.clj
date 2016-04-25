(ns priceous.zakaz
  (:require [net.cgrand.enlive-html :as html]
            [priceous.flow :as flow]
            [priceous.utils :as u]))

(defn- text-from-selector [provider node selector]
  (html/text (u/ensure-one (html/select node selector)
                           :selector selector
                           :required false
                           :provider (:name provider))))

(defn- zakaz-price [price-node provider]
  (let [text (str (text-from-selector provider price-node [:.grivna.price])
                  (text-from-selector provider price-node [:.kopeiki]))
        value (u/safe-parse-double text)]
    (if value (/ value 100.0) nil)))

(defn node->price [provider node]
  (-> node
      (html/select [:.one-product-price])
      (u/ensure-one
       :required false
       :selector [:.one-product-price]
       :provider (:name provider))
      (zakaz-price provider)))

(defn node->old-price [provider node]
  (-> node
      (html/select [:.badge.right-up-sale-bage])
      (u/ensure-one
       :required false
       :selector [:.badge.right-up-sale-bage]
       :provider (:name provider))
      (zakaz-price provider)))

(defn node->link [provider node]
  (-> node
      (html/select (:selector-link provider))
      (u/ensure-one
       :required true
       :selector (:selector-link provider)
       :provider (:name provider))
      (get-in [:attrs :href])
      ((fn [part-link] (str (:base-url provider) part-link)))))

(defn page->nodes [provider page]
  (-> page
      (html/select (:selector-pages provider))
      ((fn [items]
         (remove (fn [i]
                   (-> (html/select i [:.one-product-name])
                       (u/ensure-one
                        :required false
                        :selector [:.one-product-name]
                        :provider (:name provider))
                       (html/text)
                       (clojure.string/trim)
                       (empty?))) items)))))

(def provider
  {
   :page-start 1
   ;;   :log-item? true
   
   :selector-pages      [:.one-product]
   :selector-name       [:.one-product-name]   
   :selector-link       [:.one-product-link]
   :selector-image      [:.one-product-image :img]
   :selector-available? [:.one-product-name]
   :selector-sale?      [:.badge.right-up-sale-bage]

   :node->price node->price
   :node->old-price node->old-price
   :node->link node->link
   :page->nodes page->nodes
   
   })

(defn- extend-provider [parent child]
  (-> (merge provider child)
      (assoc :page-template (str (:base-url child) "?&page=%s"))))

(def provider-metro
  (extend-provider provider
                   {:name "Metro"
                    :base-url "https://metro.zakaz.ua/ru/whiskey/"}))
(def provider-novus
  (extend-provider provider
                   {:name "Novus"
                    :base-url "https://novus.zakaz.ua/ru/whiskey/"}))
(def provider-fozzy
  (extend-provider provider
                   {:name "Fozzy"
                    :base-url "https://fozzy.zakaz.ua/ru/whiskey/"}))
(def provider-stolichnyi
  (extend-provider provider
                   {:name "Stolichnyi"
                    :base-url "https://stolichnyi.zakaz.ua/ru/whiskey/"}))
