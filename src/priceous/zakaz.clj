(ns priceous.zakaz
  (:require [net.cgrand.enlive-html :as html]
            [priceous.flow :as flow]
            [priceous.utils :as u]))

;; Zakaz.ua specific items

(defn last-page [provider page]
  (let [value 
        (some->> (u/select-mul-required page provider
                                        [:.catalog-pagination :li :a])
                 (map html/text)
                 (map u/smart-parse-double)
                 (sort)
                 (reverse)
                 (first)
                 (int))]
    ;; default page is 1
    (or value 1)))

(defn page->nodes [provider page]
  (u/select-mul-required
   page provider
   [:.one-product]))

(defn node->document
  "Transform item html snippet (node) into document"  
  [{:keys [provider-name provider-base-url provider-icon] :as provider} node]
  (let [page node
        ;; some handy local aliases
        prop (u/property-fn provider page)
        text (u/text-fn prop)

        ;; dependent items
        price-fn (fn [selector]
                   (some-> (prop selector)
                           (html/text)
                           (u/smart-parse-double)
                           (/ 100))) 
        price (price-fn [:.one-product-price])
        old-price (price-fn [:.badge.right-up-sale-bage])]
    {
     ;; provider specific options
     :provider-name           provider-name
     :provider-base-url       provider-base-url
     :provider-icon           provider-icon
     
     ;; document
     :name                    (text [:.one-product-name])     
     :link                    (-> (prop [:.one-product-link])
                                  (get-in [:attrs :href])
                                  ((fn [part-href]
                                     (str (:provider-base-url provider) "/" part-href))))
     :image                   (-> (prop [:.one-product-image :img])
                                  (get-in [:attrs :src]))

     ;; NOT DECIDED TO GATHER YET
     ;;
     ;;     :country                 
     ;; 
     ;;     :producer
     :type                    "Whisky" ;; TODO redecide
     ;; :product-code            nil
     ;; :alcohol                 need to be parsed
     ;; :description             not much description
     :timestamp               (u/now)
     :available               true ;; zakaz everything shows as available
     ;;     :volume              need to be parsed

     :sale                    (and price
                                   old-price
                                   (> old-price price))
     
     :price                   price

     :sale-description        (if old-price (format "старая цена %s" old-price) nil)
     }))
