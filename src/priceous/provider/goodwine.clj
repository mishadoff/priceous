(ns priceous.provider.goodwine
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

(defn- get-categories [provider]
  [["Виски" "http://goodwine.com.ua/viski.html?dir=asc&p=%s"]
   ["Другие Крепкие" "http://goodwine.com.ua/drugie-krepkie.html?dir=asc&p=%s"]])

(defn- node->document
  "Read html resource from URL and transforms it to the document"  
  [provider page]
  (let [q+ (fn [selector] (su/select-one-req page provider selector))
        q* (fn [selector] (su/select-mul-req page provider selector))
        remove-h2-node
        (fn [node]
          ;; remove h2.innerTitleProd from content
          (assoc node :content (remove #(and map?
                                             (= (get % :tag) :h2)
                                             (= (get-in % [:attrs :class] "innerTitleProd")))
                                       (:content node))))
        ;; some handy local aliases
        prop (su/property-fn provider page)
        text (su/text-fn prop)
        spec-left (su/build-spec-map
                   provider page
                   [:.i-colLeft :.innerDiv [:h2 :.innerTitleProd]]
                   [:.i-colLeft :.innerDiv]
                   :vals-post-fn remove-h2-node)
        spec-right (su/build-spec-map
                    provider page
                    [:.i-colRight :.innerDiv [:h2 :.innerTitleProd]]
                    [:.i-colRight :.innerDiv]
                    :vals-post-fn remove-h2-node)
        spec (merge spec-left spec-right)
        available (empty? (su/select-one-opt page provider [:.notAvailableBlock]))]
    {
     ;; provider specific options
     :provider                (get-in provider [:info :name])
     :base-url                (get-in provider [:info :base-url])
     :icon-url                (get-in provider [:info :icon])
     :icon-url-width          (get-in provider [:info :icon-width])
     :icon-url-height         (get-in provider [:info :icon-height])
     
     ;; document
     :name                    (text [:.titleProd])
     :link                    (get-in provider [:info :base-url]) ;; TODO fix
     :image                   (-> (q+ [:.imageDetailProd [:img :#mag-thumb]])
                                  (get-in [:attrs :src]))
     :country                 (spec "география")
     :producer                (spec "Производитель")
     :type                    (spec "Тип")
     :alcohol                 (-> (spec "Крепость, %") (u/smart-parse-double))
     :description             (spec "цвет, вкус, аромат")
     :timestamp               (u/now)
     :product-code            (-> (q+ [:.titleProd :.articleProd :span]) (html/text))
     :available               available
     :volume                  (if available
                                (-> (q* [:.additionallyServe :.bottle :p])
                                    (first)
                                    (html/text)
                                    (u/smart-parse-double)))
     :price                   (if available
                                (-> (q* [:.additionallyServe :.price])
                                    (first)
                                    (html/text)
                                    (u/smart-parse-double)))
     :sale-description        nil
     :sale                    false
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
           :page-template  "http://goodwine.com.ua/viski.html?p=%s"
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :configuration {
                   :categories-fn      get-categories
                   :parallel-count     10
                   :strategy           :heavy
                   :node->document     node->document
                   :url-selector       [:.catalogListBlock [:a :.title]]
                   :last-page-selector [:.paginator [:a (html/attr-has :href)]]
                   }
   })
