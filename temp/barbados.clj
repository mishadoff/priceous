(ns priceous.provider.barbados
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Виски" "http://bar-bados.com.ua/whiskey/"]
    ["Коньяк" "http://bar-bados.com.ua/cognac/"]
    ["Абсент" "http://bar-bados.com.ua/spirits/absent/"]
    ["Арманьяк" "http://bar-bados.com.ua/spirits/armanaic/"]
    ["Бренди" "http://bar-bados.com.ua/spirits/brendi/"]
    ["Водка" "http://bar-bados.com.ua/spirits/vodka/"]
    ["Граппа" "http://bar-bados.com.ua/spirits/grappac/"]
    ["Джин" "http://bar-bados.com.ua/spirits/jin/"]
    ["Кальвадос" "http://bar-bados.com.ua/spirits/kalvados/"]
    ["Кашаса" "http://bar-bados.com.ua/spirits/kashaca/"]
    ["Настойка" "http://bar-bados.com.ua/spirits/nastoyka/"]
    ["Ром" "http://bar-bados.com.ua/spirits/rom/"]
    ["Самбука" "http://bar-bados.com.ua/spirits/sambuca/"]
    ["Текила" "http://bar-bados.com.ua/spirits/tequila-mazkal/"]
    ["Шампанское" "http://bar-bados.com.ua/champagne_c/"]
    ["Ликеры и вермуты" "http://bar-bados.com.ua/liqueurs-vermouth/"]]
   (mapv (fn [[name url]] {:name name
                           :template (str url "?page=%s&limit=1000")}))))

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
          (assoc :name (text+ [[:h1 :.ptitle-h1]]))
          (assoc :link (:link nodemap))
          (assoc :image (some-> (q? [[:img (html/attr= :itemprop "image")]])
                                (get-in [:attrs :src])))
          (assoc :country (spec "Страна:"))
          (assoc :type (-> (str (p/category-name provider) " "
                                (spec "Тип:") " "
                                (spec "Тип виски:") " ")
                           (u/cleanup)))
          (assoc :alcohol (some-> (spec "Крепость:") (u/smart-parse-double)))
          (assoc :timestamp (u/now))
          (assoc :description (text? [:#tab-product-tab1]))
          (assoc :volume (some-> (text? [:.options [:div html/first-child]])
                                 (u/smart-parse-double)))
          (assoc :available (= "Есть в наличии" (spec "Наличие:")))
          (assoc :price (some-> (q*? [[:span (html/attr= :itemprop "price")]])
                                (first)
                                (html/text)
                                (u/smart-parse-double)))
          
          ))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   ;; provider specific information
   :info {
          :name          "Barbados"
          :base-url      "http://bar-bados.com.ua/"
          :icon          "http://bar-bados.com.ua/catalog/view/theme/wine/images/logo.png"
          :icon-width    "133"
          :icon-height   "72"
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
                   :link-selector       [:.name :a]
                   :link-selector-type  :full-href
                   :last-page-selector  [:.pagination :.links #{:a :b}]
                   }
   
   })
