(ns priceous.goodwine
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.config :as config]
            [priceous.flow :as flow]
            [priceous.utils :as u]))

;; SPECIFIC
(defn- last-page
  "Detects what is the last page for pagination"
  [provider page]
  (-> page 
      (u/select [:.paginator-list [:li html/last-child]] provider)
      (html/text)
      (u/safe-parse-double)
      (int)))

;; SPECIFIC
(defn- page->urls
  ;; TODO use logged select
  [provider page]
  (map #(get-in % [:attrs :href]) (html/select page [:.g-title-bold :a])))

;; UTILS
(defn- text-contains [s sub-selector]
  (html/pred #(.contains (html/text (first (html/select % sub-selector))) s)))

;; TODO replace all html select first to utils selector
;; MOVE all helpers to utils
;; SPECIFIC
(defn- url->document
  "Read html resource from URL and transforms it to the document"  
  [{:keys [provider-name provider-base-url provider-icon] :as provider} url]
  (let [page (u/fetch url)
        prop (fn [selector postfn] (postfn (u/select page selector provider-name)))
        text (fn [selector] (prop selector html/text))
        spec (fn [field]
               (prop [[:.specifications-list-item (text-contains field [:.specifications-list-title])]]
                     #(-> (html/select % [:.specifications-list-field])
                          (first)
                          (html/text)
                          (clojure.string/trim))))]
    {
     ;; provider specific options
     :provider-name           provider-name
     :provider-base-url       provider-base-url
     :provider-icon           provider-icon
     
     ;; document
     :name                    (text [:.pp-title])
     :link                    url
     :image                   (prop [:.b-product-img-link :img] #(get-in % [:attrs :src]))
     :country                 (spec "Страна регион:")
     :producer                (spec "Производитель:")
     :type                    (spec "Тип:")
     :product-code            (spec "Артикул:")
     :alcohol                 (-> (spec "Крепость:") (u/safe-parse-double-with-intruders))
     :description             (text [:.product-description])
     :timestamp               (u/now)
     :available               (prop [:.g-status-available] boolean)
     :volume                  (prop [:.product-status-volume]
                                    #(-> (html/text %) (u/safe-parse-double-with-intruders)))
     :sale                    (prop [:.g-tag.g-tag-big.g-tag-promotion]
                                    #(-> (html/text %)
                                         (.contains "Акция")))
     :price                   (prop [:.price.price-big.price-retail]
                                    #(-> (html/text %) (u/safe-parse-double-with-intruders)))
     :sale-description        (let [sale-price 
                                    (prop [:.price.price-big.price-wholesale]
                                          #(-> (html/text %) (u/safe-parse-double-with-intruders)))]
                                (if sale-price (format "от 6 бутылок %s" sale-price) nil))
     }))


(def provider
  {:provider-name "Goodwine"
   :provider-base-url "http://goodwine.com.ua/"
   :provider-icon "http://i.goodwine.com.ua/design/goodwine-logo.png"

   :state {:page-current   1
           :page-processed 0
           :page-template "http://goodwine.com.ua/whisky/c4502/page=%s/"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :url->document url->document
   :page->urls page->urls
   :last-page  last-page
   
   })
