(ns priceous.provider.alcoparty
  (:require [net.cgrand.enlive-html :as html]
            [clojure.tools.logging :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Абсент" "https://alcoparty.com.ua/category/absent/"]
    ["Биттеры и настойки" "https://alcoparty.com.ua/category/bittery-i-nastojki/"]
    ["Вино" "https://alcoparty.com.ua/category/vino/"]
    ["Виски" "https://alcoparty.com.ua/category/viski/"]
    ["Водка" "https://alcoparty.com.ua/category/vodka/"]
    ["Джин" "https://alcoparty.com.ua/category/dzhin/"]
    ["Вино игристое" "https://alcoparty.com.ua/category/igristye-vina/"]
    ["Коньяк" "https://alcoparty.com.ua/category/konjak/"]
    ["Ликер" "https://alcoparty.com.ua/category/likyor/"]
    ["Ром" "https://alcoparty.com.ua/category/rom/"]
    ["Текила" "https://alcoparty.com.ua/category/tekila/"]
    ["Шампанское" "https://alcoparty.com.ua/category/shampanskoe/"]]
   (mapv (fn [[name url]] {:name name :template (str url "offset%s/")}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  [provider nodemap]
  (su/with-selectors provider nodemap
    (-> {}
        (assoc :provider (p/pname provider))
        (assoc :name (text+ [:.b-text]))
        (assoc :link (some-> (q+ [:.b-text :a]) (get-in [:attrs :href]) (#(u/full-href provider %))))
        (assoc :image (some-> (q? [:.categoty-product-icon :img]) (get-in [:attrs :src]) (#(u/full-href provider %))))
        (assoc :type (p/category-name provider))
        (assoc :timestamp (u/now))
        (assoc :price (some-> (q? [:.categoty-product-price])
                              (html/at [:span] nil)
                              (first)
                              (html/text)
                              (u/cleanup)
                              (u/smart-parse-double)
                              (u/force-pos)))
        ((fn [doc]
           (let [old-price (some-> (text? [:.categoty-product-price.sale :span])
                                   (u/smart-parse-double))
                 sale (boolean old-price)
                 sale-desc (if sale (format "старая цена %.2f" old-price) nil)
                 available (boolean (:price doc))]
             (-> doc
                 (assoc :sale sale)
                 (assoc :sale-description sale-desc)
                 (assoc :available available)))))
        )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Alcoparty"
          :base-url      "https://alcoparty.com.ua/"
          :icon          "/images/alcoparty.png"
          }
   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :page-template  :category-manageed
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           :current-val    0
           :init-val       0
           :advance-fn     (partial + 20)
           }
   
   :configuration {
                   :categories-fn      get-categories
                   :threads            1
                   :strategy           :light
                   :node->document     node->document
                   :node-selector      [:.product_brief_block]
                   :last-page-selector [:.catnavigator #{:a :b}]
                   }
   })
