(ns priceous.provider.megamarket
  (:require [net.cgrand.enlive-html :as html]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]))

;; FIXME (do not request all data at once)
(defn get-categories [provider]
  [["Бальзам" "https://megamarket.ua/catalogue/category/1010?show=48000&page=%s"]
   ["Виски" "https://megamarket.ua/catalogue/category/1020?show=48000&page=%s"]
   ["Вермут" "https://megamarket.ua/catalogue/category/1030?show=48000&page=%s"]
   ["Водка" "https://megamarket.ua/catalogue/category/1050?show=48000&page=%s"]
   ["Джин" "https://megamarket.ua/catalogue/category/1060?show=48000&page=%s"]
   ["Коньяк" "https://megamarket.ua/catalogue/category/1070?show=48000&page=%s"]
   ["Ликеры" "https://megamarket.ua/catalogue/category/1080?show=48000&page=%s"]
   ["Ром" "https://megamarket.ua/catalogue/category/1100?show=48000&page=%s"]])

(defn node->document
  "Transform item html snippet (node) into document"  
  [provider page ctx]
  (let [;; some handy local aliases
        prop (su/property-fn provider page)
        text (su/text-fn prop)
        sale-price (-> (text [:.price.old-price])
                       (u/smart-parse-double))
        price (cond
                sale-price (-> (text [:.price_gross]) (u/smart-parse-double))
                :else (-> (text [:.price]) (u/smart-parse-double)))]
    {
     :provider                (get-in provider [:info :name])
     :base-url                (get-in provider [:info :base-url])
     :icon-url                (get-in provider [:info :icon])
     :icon-url-width          (get-in provider [:info :icon-width])
     :icon-url-height         (get-in provider [:info :icon-height])
     
     ;; document
     :name                    (text [:.product :> :a])     
     :link                    (-> (prop [:.product :> :a])
                                  (get-in [:attrs :href])
                                  (#(u/full-href provider %)))
     :image                   (-> (prop [:.img :a :img])
                                  (get-in [:attrs :src])
                                  (#(u/full-href provider %)))
     
     :timestamp               (u/now)
     :available               true
     :sale                    (re-seq #"Акционный товар" (text [:.product-info]))
     
     ;;:type                    nil ;; no type for now
     :price                   price
     :sale-description        (if sale-price (format "старая цена %.2f" sale-price))
     }))


(defn get-categories [provider]
  [["Бальзам" "https://megamarket.ua/catalogue/category/1010?show=48000&page=%s"]
   ["Виски" "https://megamarket.ua/catalogue/category/1020?show=48000&page=%s"]
   ["Вермут" "https://megamarket.ua/catalogue/category/1030?show=48000&page=%s"]
   ["Водка" "https://megamarket.ua/catalogue/category/1050?show=48000&page=%s"]
   ["Джин" "https://megamarket.ua/catalogue/category/1060?show=48000&page=%s"]
   ["Коньяк" "https://megamarket.ua/catalogue/category/1070?show=48000&page=%s"]
   ["Ликеры" "https://megamarket.ua/catalogue/category/1080?show=48000&page=%s"]
   ["Ром" "https://megamarket.ua/catalogue/category/1100?show=48000&page=%s"]])


;;; Provider Info

(def provider
  {:info {
          :name "Megamarket"
          :base-url "https://megamarket.ua"
          :icon "https://megamarket.ua/style/img/logo.png"
          :icon-width "91"
          :icon-height "34"
          }

   :state {
           :page-current   1
           :page-processed 0
           :page-template "https://megamarket.ua/catalogue?page=%s"
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :configuration {
                   :categories-fn       get-categories
                   :parallel-count      1
                   :strategy            :light
                   :node->document      node->document
                   :node-selector       [:.product]
                   :last-page-selector  :one-page
                   }
   
   })
