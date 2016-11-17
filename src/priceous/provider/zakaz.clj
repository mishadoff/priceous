(ns priceous.provider.zakaz
  (:require [net.cgrand.enlive-html :as html]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]
            ))

;; Zakaz.ua specific items
(defn last-page [provider page]
  (let [value 
        (some->> (su/select-mul-req page provider
                                    [:.catalog-pagination [:a (html/attr-has :href)]])
                 (map html/text)
                 (remove #{"»"})
                 (map u/smart-parse-double)
                 (sort)
                 (last)
                 (int))]
    ;; default page is 1
    (or value 1)))

(defn page->nodes [provider page]
  (su/select-mul-req page provider [:.one-product]))

(defn node->document
  "Transform item html snippet (node) into document"  
  [provider node]
  (let [page node
        ;; some handy local aliases
        prop (su/property-fn provider page)
        text (su/text-fn prop)

        ;; dependent items
        price-fn (fn [selector]
                   (some-> (prop selector)
                           (html/text)
                           (u/smart-parse-double)
                           (/ 100))) 
        price (price-fn [:.one-product-price])
        old-price (price-fn [:.badge.right-up-sale-bage])]
    {
     :provider                (get-in provider [:info :name])
     :base-url                (get-in provider [:info :base-url])
     :icon-url                (get-in provider [:info :icon])
     :icon-url-width          (get-in provider [:info :icon-width])
     :icon-url-height         (get-in provider [:info :icon-height])
     
     ;; document
     :name                    (text [:.one-product-name])     
     :link                    (-> (prop [:.one-product-link])
                                  (get-in [:attrs :href])
                                  ((fn [part-href]
                                     (str (get-in provider [:info :base-url]) "/" part-href))))
     :image                   (-> (prop [:.one-product-image :img])
                                  (get-in [:attrs :src]))
     :type                    nil
     :timestamp               (u/now)
     :available               true
     :sale                    (and price old-price (> old-price price))
     :price                   price
     :sale-description        (if old-price (format "старая цена %.2f" old-price) nil)
     }))
