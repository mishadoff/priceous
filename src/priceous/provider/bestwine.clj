(ns priceous.provider.bestwine
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Все товары" "http://best-wine.com.ua/catalog/cid1/"]

    ;; ["Вино" "http://best-wine.com.ua/catalog/cid1/14/"]
    ;; ["Коньяк" "http://best-wine.com.ua/catalog/cid1/15/"]
    ;; ["Виски" "http://best-wine.com.ua/catalog/cid1/16/"]
    ;; ["Текила" "http://best-wine.com.ua/catalog/cid1/17/"]
    ;; ["Ром" "http://best-wine.com.ua/catalog/cid1/18/"]
    ;; ["Прочее" "http://best-wine.com.ua/catalog/cid1/19/"]
    ;; ["Водка" "http://best-wine.com.ua/catalog/cid1/49/"]
    ;; ["Джин" "http://best-wine.com.ua/catalog/cid1/50/"]
    ;; ["Ликер" "http://best-wine.com.ua/catalog/cid1/52/"]
    ;; ["Граппа" "http://best-wine.com.ua/catalog/cid1/60/"]
    ;; ["Абсент" "http://best-wine.com.ua/catalog/cid1/61/"]
    ;; ["Вермут" "http://best-wine.com.ua/catalog/cid1/51/"]
    ;; ["Херес" "http://best-wine.com.ua/catalog/cid1/72/"]
    ;; ["Самбука" "http://best-wine.com.ua/catalog/cid1/73/"]
    ;; ["Виски Бурбон" "http://best-wine.com.ua/catalog/cid1/58/"]
    ;; ["Шампанское" "http://best-wine.com.ua/catalog/cid1/81/"]
    ;; ["Бренди" "http://best-wine.com.ua/catalog/cid1/82/"]
    ]
   (mapv (fn [[name url]] {:name name :template (str url "page%s")}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  [provider nodemap]
  (su/with-selectors provider nodemap
    (-> {}
        (assoc :provider (p/pname provider))
        (assoc :name (text+ [:.productListName]))
        (assoc :link (some-> (q+ [:.productListName :a])
                             (get-in [:attrs :href])))
        (assoc :image (-> (q+ [:.productListImage :img])
                          (get-in [:attrs :src])))

        ;; TODO
        #_(assoc :type (p/category-name provider))
        #_(assoc :alcohol (-> (spec "Крепость, %") (u/smart-parse-double)))
        #_(assoc :description (spec "цвет, вкус, аромат"))

        (assoc :timestamp (u/now))
        (assoc :available (boolean (some->> (text+ [:.productListInfo])
                                            (re-seq #"В наличии"))))
        (assoc :price (-> (text+ [:.price])
                          (u/smart-parse-double)
                          (u/force-pos)))    
        )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Bestwine"
          :base-url      "http://best-wine.com.ua/"
          :icon          "/images/bestwine.png"
          :icon-width    "263"
          :icon-height   "84"
          }
   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :page-template  :category-manageed
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           
           :current-val    1
           :init-val       1
           :advance-fn     inc

           }
   
   :configuration {
                   :categories-fn      get-categories
                   :threads            8
                   :strategy           :light
                   :node->document     node->document
                   :node-selector      [:.productListEl]
                   :last-page-selector [:.paginator :a]
                   }
   })

