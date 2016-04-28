(ns priceous.rozetka
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]))

(def last-page (u/generic-last-page
                [[:ul (html/attr= :name "paginator")]
                 [:li html/last-child] :span]))


(def page->urls (u/generic-page-urls [:.g-i-tile-i-title :a]))

(defn- parse-price-from-rozetka-json [s]
  (some->> (clojure.string/replace s #"%.." " ")
           (re-seq #" price *([0-9\\.]+) *price_formatted ")
           (first)
           (second)
           (u/smart-parse-double)))

(defn- parse-old-price-from-rozetka-json [s]
  (some->> (clojure.string/replace s #"%.." " ")
           (re-seq #" old_price *([0-9\\.]+) *old_price_formatted ")
           (first)
           (second)
           (u/smart-parse-double)
           ((fn [value] (if (zero? value) nil value)))))


(defn- url->document
  "Read html resource from URL and transforms it to the document"  
  [{:keys [provider-name provider-base-url provider-icon] :as provider} url]
  (let [page (u/fetch url)
        ;; some handy local aliases
        prop (u/property-fn provider page)
        text (u/text-fn prop)
        spec (u/build-spec-map provider page
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
     :provider-name           provider-name
     :provider-base-url       provider-base-url
     :provider-icon           provider-icon
     
     ;; document
     :name                    name
     :link                    url
     :image                   (-> (prop [:#detail_image_container :.responsive-img :img])
                                  (get-in [:attrs :src]))

     :country                 (str (spec "Страна") " " (spec "Регион"))

     :producer                (some-> (u/select-mul-optional
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
  {:provider-name "Rozetka"
   :provider-base-url "http://rozetka.com.ua/"
   :provider-icon "http://i1.rozetka.ua/logos/0/99.png"

   :state {:page-current   1
           :page-processed 0
           :page-template "http://rozetka.com.ua/krepkie-napitki/c4594292/filter/page=%s;vid-napitka-69821=whiskey-blend,whiskey-bourbon,whiskey-single-malt/"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :strategy :heavy
   
   :page->urls page->urls
   :url->document url->document

   :last-page  last-page
   
   })
