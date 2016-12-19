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
  "Transform enlive node to provider specific document using context"  
  [provider page ctx]
  (let [;; these are common
        prop (su/property-fn provider page)
        text (su/text-fn prop)
        q+ (fn [selector] (su/select-one-req page provider selector))
        q* (fn [selector] (su/select-mul-req page provider selector))
        
        ;; these are specific to goodwine
        remove-h2-node
        (fn [node]
          ;; remove h2.innerTitleProd from content
          (assoc node :content (remove #(and map? 
                                             (= (get % :tag) :h2)
                                             (= (get-in % [:attrs :class] "innerTitleProd")))
                                       (:content node))))
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
        
        ]
    (->
     {} ;; start with empty document
     (assoc :provider        (get-in provider [:info :name]))
     (assoc :base-url        (get-in provider [:info :base-url]))
     (assoc :icon-url        (get-in provider [:info :icon]))
     (assoc :icon-url-width  (get-in provider [:info :icon-width]))
     (assoc :icon-url-height (get-in provider [:info :icon-height]))
     (assoc :name            (text [:.titleProd]))
     (assoc :link            (:url ctx))
     (assoc :image           (-> (q+ [:.imageDetailProd [:img :#mag-thumb]])
                                 (get-in [:attrs :src])))
     (assoc :country         (spec "география"))
     (assoc :producer        (spec "Производитель"))
     (assoc :type            (spec "Тип"))
     (assoc :alcohol         (-> (spec "Крепость, %") (u/smart-parse-double)))
     (assoc :description     (spec "цвет, вкус, аромат"))
     (assoc :timestamp       (u/now)) ;; maybe parsing date better?
     (assoc :product-code    (-> (q+ [:.titleProd :.articleProd :span]) (html/text)))
     (assoc :available       (empty? (su/select-one-opt page provider [:.notAvailableBlock])))

     ;; volume and price blocks are present if product is available
     ((fn [p] (assoc p :volume
                     (if (:avalable p)
                       (-> (q* [:.additionallyServe :.bottle :p])
                           (first)
                           (html/text)
                           (u/smart-parse-double))))))

     ((fn [p] (assoc p :price
                     (if (:avalable p)
                       (-> (q* [:.additionallyServe :.price])
                           (first)
                           (html/text)
                           (u/smart-parse-double))))))
     
     (assoc :sale-description nil)
     (assoc :sale             false)
     )))

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
                   :url-selector-type  :full-href
                   :last-page-selector [:.paginator [:a (html/attr-has :href)]]
                   }
   })
