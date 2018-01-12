(ns priceous.provider.megamarket
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->> [["Бальзам" "https://megamarket.ua/catalogue/category/1010"]
        ["Виски" "https://megamarket.ua/catalogue/category/1020"]
        ["Вермут" "https://megamarket.ua/catalogue/category/1030"]
        ["Вино" "https://megamarket.ua/catalogue/category/1040"]
        ["Водка" "https://megamarket.ua/catalogue/category/1050"]
        ["Джин" "https://megamarket.ua/catalogue/category/1060"]
        ["Коньяк" "https://megamarket.ua/catalogue/category/1070"]
        ["Ликеры" "https://megamarket.ua/catalogue/category/1080"]
        ["Пиво" "https://megamarket.ua/catalogue/category/1090"]
        ["Ром" "https://megamarket.ua/catalogue/category/1100"]
        ["Шампанское" "https://megamarket.ua/catalogue/category/1110"]]
       (mapv (fn [[name template]] {:name name :template (str template "?page=%s")}))
       ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  "Transform enlive node to provider specific document using node context.
  For heavy strategy, in context available whole :page, partial :node
  and :link by which whole page was retrieved.

  If scrapper finds a broken page, it returns whole doc as a nil."
  [provider nodemap]
  (su/with-selectors provider nodemap
    (-> {}
        (assoc :provider (p/pname provider))
        (assoc :name (text+ [:.product :> :a]))
        (assoc :link (-> (q+ [:.product :> :a])
                         (get-in [:attrs :href])
                         (#(u/full-href provider %))))
        (assoc :image (-> (q+ [:.img :a :img])
                          (get-in [:attrs :src])
                          (#(u/full-href provider %))))
        (assoc :type (p/category-name provider))
        (assoc :timestamp (u/now))
        (assoc :available true)
        (assoc :sale (re-seq #"Акционный товар" (or (text? [:.product-info]) "")))

        ((fn [doc]
           (let [sale-price (some-> (text? [:.price.old-price])
                                    (u/smart-parse-double))
                 price (cond sale-price (some-> (text? [:.price_gross])
                                                (u/smart-parse-double))
                             :else (some-> (text? [:.price])
                                           (u/smart-parse-double)))
                 sale-description (if sale-price (format "старая цена %.2f" sale-price))]
             (-> doc
                 (assoc :price price)
                 (assoc :sale-description sale-description)))))

        )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {:info {
          :name "Megamarket"
          :base-url "https://megamarket.ua"
          :icon "https://megamarket.ua/style/img/logo.png"
          :icon-width "91"
          :icon-height "34"
          }

   :state {
           :page-current   1
           :page-processed 0
           :page-template "https://megamarket.ua/catalogue?page=%s"
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           :init-val       1
           :current-val    1
           :advance-fn     inc
           }

   :configuration {
                   :categories-fn       get-categories
                   :parallel-count      1
                   :strategy            :light
                   :node->document      node->document
                   :node-selector       [:.product]
                   :last-page-selector  [:#yw0 [:li html/last-child] :a]
                   :last-page-process-fn (fn [node]
                                           (some->> (get-in node [:attrs :href])
                                                    (re-seq #"page=(\d+)")
                                                    (first)
                                                    (second)))
                   }

   })
