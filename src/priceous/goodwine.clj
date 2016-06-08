(ns priceous.goodwine
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]))

(def ^{:private true} next-page?
  (u/generic-last-page? [:.paginator-list [:li html/last-child]]))

(def ^{:private true} page->urls
  (u/generic-page-urls [:.g-title-bold :a]))

(defn- url->document
  "Read html resource from URL and transforms it to the document"  
  [provider url]
  (let [page (u/fetch url) ;; retrieve the page
        ;; some handy local aliases
        prop (u/property-fn provider page)
        text (u/text-fn prop)
        spec (u/build-spec-map provider page
                               [:.specifications-list-title]
                               [:.specifications-list-field])]
    {
     ;; provider specific options
     :provider                (get-in provider [:info :name])
     
     ;; document
     :name                    (text [:.pp-title])
     :link                    url
     :image                   (-> (prop [:.b-product-img-link :img])
                                  (get-in [:attrs :src]))
     :country                 (spec "Страна регион:")
     :producer                (spec "Производитель:")
     :type                    (spec "Тип:")
     :product-code            (spec "Артикул:")
     :alcohol                 (-> (spec "Крепость:") (u/smart-parse-double))
     :description             (text [:.product-description])
     :timestamp               (u/now)
     :available               (-> (prop [:.g-status-available]) boolean)
     :volume                  (-> (prop [:.product-status-volume])
                                  (html/text)
                                  (u/smart-parse-double))
     :sale                    (-> (prop [:.g-tag.g-tag-big.g-tag-promotion])
                                  (html/text)
                                  (.contains "Акция"))
     :price                   (-> (prop [:.price.price-big.price-retail])
                                  (html/text)
                                  (u/smart-parse-double))
     :sale-description        (let [sale-price (-> (prop [:.price.price-big.price-wholesale])
                                                   (html/text)
                                                   (u/smart-parse-double))]
                                (if sale-price (format "от 6 бутылок %s" sale-price) nil))
     }))


;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;  PROVIDER  ;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;


(def provider
  {
   ;; provider specific information
   :info {
          :name          "Goodwine"
          :base-url      "http://goodwine.com.ua/"
          :icon          "http://i.goodwine.com.ua/design/goodwine-logo.png"
          :icon-width    "70"
          :icon-height   "34"
          }
   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :page-template "http://goodwine.com.ua/whisky/c4502/page=%s/"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }
   ;; fetch strategy defines how we will fetch results
   :fetch-strategy :heavy
   
   :functions {
               :url->document url->document
               :page->urls    page->urls
               :last-page?    last-page?
               }
   })
