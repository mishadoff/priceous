(ns priceous.rozetka
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]
            [cheshire.core :as json]))

(def next-page? (su/generic-next-page?
                 [[:ul (html/attr= :name "paginator")]
                  [:li html/last-child] :span]))

(def page->urls (su/generic-page-urls [:.g-i-tile-i-title :a]))

(defn- parse-price-from-rozetka-json [s]
  (-> (re-seq #"var pricerawjson = \"(.*)\";" s)
      (first)
      (second)
      (java.net.URLDecoder/decode)
      (json/parse-string true)
      :price
      (u/smart-parse-double)))

(defn- parse-old-price-from-rozetka-json [s]
  (-> (re-seq #"var pricerawjson = \"(.*)\";" s)
      (first)
      (second)
      (java.net.URLDecoder/decode)
      (json/parse-string true)
      :old_price
      (u/smart-parse-double)))

(defn- url->document
  "Read html resource from URL and transforms it to the document"  
  [{:keys [provider-name provider-base-url provider-icon
           provider-icon-w provider-icon-h] :as provider} url]
  (let [page (u/fetch url)
        ;; some handy local aliases
        prop (su/property-fn provider page)
        text (su/text-fn prop)
        spec (su/build-spec-map provider page
                               [:.detail-chars-l-i-title]
                               [:.detail-chars-l-i-field])

        ;; dependent properties
        name (text [:.detail-title])
        priceblock (prop [[:div (html/attr= :name "block_desc")] 
                          [:script (html/pred #(.contains (html/text %) "pricerawjson"))]])
        old-price (-> priceblock
                      (html/text)
                      (parse-old-price-from-rozetka-json))
        price      (-> priceblock
                       (html/text)
                       (parse-price-from-rozetka-json))
        ]
    {
     ;; provider specific options
     :provider                (get-in provider [:info :name])
     :base-url                (get-in provider [:info :base-url])
     :icon-url                (get-in provider [:info :icon])
     :icon-url-width          (get-in provider [:info :icon-width])
     :icon-url-height         (get-in provider [:info :icon-height])

     ;; document
     :name                    name
     :link                    url
     :image                   (-> (prop [:#detail_image_container :.responsive-img :img])
                                  (get-in [:attrs :src]))

     :country                 (str (spec "Страна") " " (spec "Регион"))

     :producer                (some-> (su/select-mul-opt
                                       page
                                       provider
                                       [:.breadcrumbs-i :.breadcrumbs-title])
                                      (last)
                                      (html/text)
                                      (u/cleanup))

     :type                    (spec "Категория")
     :product-code            (text [[:span (html/attr= :name "goods_code")]])
     :alcohol                 (-> (spec "Крепость") (u/smart-parse-double))

     :description             (str (text [:#short_text]) "\n"
                                   (spec "Вкус") "\n"
                                   (spec "Аромат"))

     :timestamp               (u/now)

     :available               (-> (prop [:.detail-status])
                                  (html/text)
                                  (u/cleanup)
                                  (= "Нет в наличии")
                                  (not))

     :volume                  (-> (spec "Объем") (u/smart-parse-double))

     :sale                    (and price
                                   old-price
                                   (> old-price price))
     
     :price                   price

     :sale-description        (let []
                                 (if old-price (format "старая цена %s" old-price) nil))
     }))





;;;;;;; Provider description

(def provider
  {
   ;; provider specific information
   :info {
          :name "Rozetka"
          :base-url "http://rozetka.com.ua/"
          :icon "http://i1.rozetka.ua/logos/0/99.png"
          :icon-width "134"
          :icon-heught "34"
          }
   
   :state {
           :page-current   1
           :page-processed 0
           :page-template  "http://rozetka.com.ua/krepkie-napitki/c4594292/filter/page=%s;vid-napitka-69821=whiskey-blend,whiskey-bourbon,whiskey-single-malt/"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :fetch-strategy :heavy

   :functions {
               :url->document url->document
               :page->urls page->urls
               :last-page?  next-page?
               }
   
   })
