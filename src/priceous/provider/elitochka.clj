(ns priceous.provider.elitochka
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]))

(defn get-categories [provider]
  (->> (su/select-mul-req (u/fetch "http://elitochka.com.ua/")
                          provider [:.journal-menu :li :a])
       (map (fn [node] [(html/text node)
                        (str (u/full-href provider (get-in node [:attrs :href])) "?page=%s")]))))

(defn node->document
  "Read html resource from URL and transforms it to the document"  
  [provider page]
  (let [q+ (fn [selector]
             (su/select-one-req page provider selector))
        q* (fn [selector]
             (su/select-mul-req page provider selector))
        ;; some handy local aliases
        prop (su/property-fn provider page)
        text (su/text-fn prop)
        spec (su/build-spec-map provider page
                                [:.attribute :tr [:td html/first-child]]
                                [:.attribute :tr [:td html/last-child]])
        
        price (-> (text [:.price-new]) (u/smart-parse-double))
        old-price (-> (text [:.price-old]) (u/smart-parse-double))]
    {
     ;; provider specific options
     :provider                (get-in provider [:info :name])
     :base-url                (get-in provider [:info :base-url])
     :icon-url                (get-in provider [:info :icon])
     :icon-url-width          (get-in provider [:info :icon-width])
     :icon-url-height         (get-in provider [:info :icon-height])
     
     ;; document
     :name                    (text [:.heading-title])
     :link                    (get-in provider [:info :base-url])
     :image                   (-> (q+ [:.image [:img :#image]])
                                  (get-in [:attrs :src]))

     :country                 (spec "Страна производитель:")
     :type                    (spec "Тип:")

     :alcohol                 (-> (spec "Крепость:") (u/smart-parse-double))
     :description             (str (text [:.category1_descr]) "/n"
                                   (text [:.category2_descr]))
     :timestamp               (u/now)
     
     :volume                  (-> (spec "Объем:") (u/smart-parse-double))

     :available               (->> (text [:.price [:span (html/attr= :itemprop "availability")]])
                                   (re-seq #"В наличии")
                                   (boolean))

     :price                   price
     :old-price               old-price 
     :sale                    (boolean old-price)
     :sale-description        (if (and price old-price (< price old-price))
                                (format "старая цена %.2f" old-price))

     }))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;  PROVIDER  ;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;


(def provider
  {
   ;; provider specific information
   :info {
          :name          "Elitochka"
          :base-url      "http://elitochka.com.ua/"
          :icon          "/images/elitochka.png"
          :icon-width    "94"
          :icon-height   "34"
          }
   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :page-template  :category-managed
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :configuration {
               :categories-fn       get-categories
               :parallel-count      10
               :strategy            :heavy
               :node->document      node->document
               :url-selector        [:.product-list :.image :a]
               :url-selector-type   :relative-to-base
               :last-page-selector  [:.pagination :.links #{:a :b}]
               }
   
   })
