(ns priceous.provider.elitochka
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]
            [clj-http.client :as http]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-categories [provider]
  (->> [["Абсент" 332]
        ["Бренди" 224]
        ["Вермут" 333]
        ["Вино"   18]
        ["Виски"  15]
        ["Водка"  16]
        ["Джин"   17]
        ["Кальвадос" 156]
        ["Коньяк" 20]
        ["Ликер" 21]
        ["Текила" 24]
        ["Шампанское" 19]
        ]
       (mapv (fn [[cat number]]
               {:name cat
                :template (str "https://elitochka.com.ua/?"
                               "route=product/category/getcatalogproducts"
                               (format "&path=%d" number) "&page=%s")}))
       ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn node->document
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
          (assoc :available (boolean (some->> (text? [[:span (html/attr= :itemprop "availability")]])
                                              (re-seq #"В наличии"))))

          (assoc :description (text? [:.category1_descr]))

          ;; prices
          ((fn [doc]
             (let [price (some-> (q? [[:meta (html/attr= :itemprop "price")]])
                                 (get-in [:attrs :content])
                                 (u/smart-parse-double))
                   oldprice (some-> (html/at (:page nodemap) [:#block-related] nil)
                                    (html/select [:.price-old])
                                    (first)
                                    (html/text)
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

(defn fetch-products [provider]
  (let [resp (http/post (p/current-page provider)
                        {:headers {"X-Requested-With" "XMLHttpRequest"}})]
    (html/html-snippet (:body resp))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   ;; provider specific information
   :info {
          :name          "Elitochka"
          :base-url      "https://elitochka.com.ua/"
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
                   :fetch-page-fn       fetch-products
                   :strategy            :heavy
                   :node->document      node->document
                   :node-selector       [:.product-list :> :div]
                   :link-selector       [:.image :a]
                   :link-selector-type  :relative
                   :last-page-selector  [:.pagination :.links #{:a :b}]
               }

   })
