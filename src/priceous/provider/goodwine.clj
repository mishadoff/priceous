(ns priceous.provider.goodwine
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]))

(def ^{:private true} next-page?
  (su/generic-next-page? [:.paginator-list [:li html/last-child]]))

(def ^{:private true} page->urls
  (su/generic-page-urls [:.g-title-bold :a]))

(defn- url->document
  "Read html resource from URL and transforms it to the document"  
  [provider url]
  (if ((get-in provider [:skip :url] #{}) url)
    (do (log/warn "Skipping URL: " url) nil)
    (let [page (u/fetch url) ;; retrieve the page
        ;; some handy local aliases
        prop (su/property-fn provider page)
        text (su/text-fn prop)
        spec (su/build-spec-map provider page
                               [:.specifications-list-title]
                               [:.specifications-list-field])
        sale-price (-> (prop [:.price.price-big.price-wholesale])
                       (html/text)
                       (u/smart-parse-double))]
    {
     ;; provider specific options
     :provider                (get-in provider [:info :name])
     :base-url                (get-in provider [:info :base-url])
     :icon-url                (get-in provider [:info :icon])
     :icon-url-width          (get-in provider [:info :icon-width])
     :icon-url-height         (get-in provider [:info :icon-height])
     
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
     :price                   (-> (prop [:.price.price-big.price-retail])
                                  (html/text)
                                  (u/smart-parse-double))
     :sale-description        (if sale-price (format "от 6 бутылок %s" sale-price) nil)
     :sale                    (boolean sale-price)
     })))


(defn get-categories [provider]
  (->> (su/select-mul-req
        (u/fetch "http://goodwine.com.ua/spirits/c34290/")
        provider
        [:.catalog-menu-table :li :a])
       (map #(vec [(html/text %)
                   (str (get-in % [:attrs :href]) "page=%s/")]))
       (cons ["Ликеры" "http://goodwine.com.ua/liqueurs/c4509/" ])
       ))

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
   :category true
   
   :functions {
               :url->document url->document
               :page->urls    page->urls
               :last-page?    next-page?

               :categories    get-categories ;; return name [tempalte]
               }

   :skip {:url #{"http://goodwine.com.ua/armagnac/p53825/"
                 "http://goodwine.com.ua/sambuca/p45499/"}}
   
   })
