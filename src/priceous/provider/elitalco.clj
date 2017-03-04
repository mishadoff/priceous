(ns priceous.provider.elitalco
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Абсент" "http://elit-alco.com.ua/catalog/absent-absinth"]
    ["Вино" "http://elit-alco.com.ua/catalog/vino"]
    ["Бренди" "http://elit-alco.com.ua/catalog/brandi-brandy"]
    ["Виски" "http://elit-alco.com.ua/catalog/viski-whiskey"]
    ["Водка" "http://elit-alco.com.ua/catalog/vodka-vodka"]
    ["Джин" "http://elit-alco.com.ua/catalog/dzhin-jin"]
    ["Кальвадос" "http://elit-alco.com.ua/catalog/kalvados-calvados"]
    ["Коньяк" "http://elit-alco.com.ua/catalog/konyak-cognac"]
    ["Ликеры и самбука" "http://elit-alco.com.ua/catalog/likery-i-sambuka-liqueur-sambuca"]
    ["Ром" "http://elit-alco.com.ua/catalog/rom-rum-ron"]
    ["Текила" "http://elit-alco.com.ua/catalog/tekila-tequila"]
    ["Шампанское и вермуты" "http://elit-alco.com.ua/catalog/shampanskoe-i-vermut-champagne-wermut"]]
    (mapv (fn [[name url]] {:name name :template (str url "/page/%s")}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  "Read html resource from URL and transforms it to the document"  
  [provider nodemap]
  (su/with-selectors provider nodemap
    (let [spec (->> (q*? [[:.text (html/attr= :itemprop "description")] :p])
                    (map html/text)
                    (map (fn [s]
                           (some->> (.split s ":" 2)
                                    (seq)
                                    (map u/cleanup)
                                    (into []))))
                    (filter (fn [v] (= (count v) 2)))
                    (into {}))]
      (-> {}
          (assoc :provider (p/pname provider))
          (assoc :name (text+ [[:h1 (html/attr= :itemprop "name")]]))
          (assoc :link (:link nodemap))
          (assoc :image (some-> (q+ [[:img (html/attr= :itemprop "image")]])
                                (get-in [:attrs :src])
                                (#(u/full-href provider %))))
          
          (assoc :country (spec "Страна"))
          (assoc :type (str (p/category-name provider) " " (spec "Тип")))
          (assoc :alcohol (some-> (spec "Крепость") (u/smart-parse-double)))
          (assoc :timestamp (u/now))
          (assoc :volume (some-> (spec "Объем") (u/smart-parse-double)))

          ;; TODO no description
          #_(assoc :description (text? [:.category1_desc]))

          ;; prices
          ((fn [doc]
             (let [price (some-> (text? [:.product :.price [:span (html/attr= :itemprop "price")]])
                                 (u/smart-parse-double))
                   oldprice (some-> (text? [:.product :.oldprice :span])
                                    (u/smart-parse-double))]
               (-> doc
                   (assoc :sale (boolean oldprice))
                   (assoc :sale-description (if oldprice (format "старая цена %.2f" oldprice)))
                   (assoc :price price)
                   (assoc :available (boolean price))))))
          
          ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   ;; provider specific information
   :info {
          :name          "Elitalco"
          :base-url      "http://elit-alco.com.ua/"
          :icon          "/images/elitalco.png"
          :icon-width    "202"
          :icon-height   "68"
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
                   :node-selector       [:.catalog :.items :.item]
                   :link-selector       [:.img :a]
                   :link-selector-type  :relative
                   :last-page-selector  [:.oPager :> #{:i :a}]
               }
   
   })
