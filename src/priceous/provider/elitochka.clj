(ns priceous.provider.elitochka
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->> (su/select*+ (u/fetch "http://elitochka.com.ua/")
                    provider
                    [:.journal-menu :li :a])
       (map (fn [node]
              {:name (html/text node)
               :template (str (u/full-href provider (get-in node [:attrs :href]))
                              "?page=%s#breadcrumb")}))
       ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  "Read html resource from URL and transforms it to the document"  
  [provider nodemap]
  (su/with-selectors provider nodemap
    (let [spec (->> [(q*? [:.attribute :tr [:td html/first-child]])
                     (q*? [:.attribute :tr [:td html/last-child]])]
                    (apply mapv (fn [a b] [(-> a html/text u/cleanup)
                                           (-> b html/text u/cleanup)]))
                    (into {}))]
      (-> {}
          (assoc :provider (p/pname provider))
          (assoc :name (text+ [:.heading-title]))
          (assoc :link (:link nodemap))
          (assoc :image (some-> (q+ [:.image [:img :#image]])
                                (get-in [:attrs :src])))
          (assoc :country (spec "Страна производитель:"))
          (assoc :type (str (p/category-name provider) " " (spec "Тип:")))
          (assoc :alcohol (some-> (spec "Крепость:") (u/smart-parse-double)))
          (assoc :timestamp (u/now))
          (assoc :volume (some-> (spec "Объем:") (u/smart-parse-double)))
          (assoc :available (boolean (some->> (text? [:.price [:span (html/attr= :itemprop "availability")]]) (re-seq #"В наличии"))))

          (assoc :description (text? [:.category1_desc]))
          
          ;; prices
          ((fn [doc]
             (let [price (some-> (text? [:.price [:span (html/attr= :itemprop "price")]])
                                 (u/smart-parse-double))
                   oldprice (some-> (text? [:.price-old])
                                    (u/smart-parse-double))
                   sale (boolean oldprice)
                   sale-desc (if (and price oldprice (< price oldprice))
                               (format "старая цена %.2f" oldprice))]
             (-> doc
                 (assoc :sale sale)
                 (assoc :sale-description sale-desc)
                 (assoc :price price)))))
          
          ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
           :current-val    1
           :init-val       1
           :advance-fn     inc
           }

   :configuration {
                   :categories-fn       get-categories
                   :threads             8
                   :strategy            :heavy
                   :node->document      node->document
                   :node-selector       [:.product-list :> :div]
                   :link-selector       [:.image :a]
                   :link-selector-type  :relative
                   :last-page-selector  [:.pagination :.links #{:a :b}]
               }
   
   })
