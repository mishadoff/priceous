(ns priceous.provider.alcovegas
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Виски" "http://alcovegas.com.ua/c36424-viski.html"]
    ["Абсент" "http://alcovegas.com.ua/c36361-absent.html"]
    ["Бренди" "http://alcovegas.com.ua/c36421-brendi.html"]
    ["Вермут" "http://alcovegas.com.ua/c36422-vermut.html"]
    ["Вино" "http://alcovegas.com.ua/c112689-vino.html"]
    ["Водка" "http://alcovegas.com.ua/c36425-vodka.html"]
    ["Граппа" "http://alcovegas.com.ua/c217212-grappa.html"]
    ["Джин" "http://alcovegas.com.ua/c36426-dzhin.html"]
    ["Кальвадос" "http://alcovegas.com.ua/c343826-kalvados-calvados.html"]
    ["Коньяк" "http://alcovegas.com.ua/c36427-konyak.html"]
    ["Ликер" "http://alcovegas.com.ua/c36428-liker.html"]
    ["Настойка" "http://alcovegas.com.ua/c41228-nastoyka.html"]
    ["Ром" "http://alcovegas.com.ua/c36429-rom.html"]
    ["Самбука" "http://alcovegas.com.ua/c36430-sambuka.html"]
    ["Текила" "http://alcovegas.com.ua/c36431-tekila.html"]
    ["Шампанское" "http://alcovegas.com.ua/c36432-shampanskoe.html"]]
   (mapv (fn [[name url]] {:name name :template (str url "?view=all")}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  [provider nodemap]
  (su/with-selectors provider nodemap
    (let [spec (->> [(q*? [:.dl-horizontal :dt])
                     (q*? [:.dl-horizontal :dd])]
                    (apply mapv (fn [a b] [(-> a html/text u/cleanup)
                                           (-> b html/text u/cleanup)]))
                    (into {}))]
      (-> {}
          (assoc :provider (p/pname provider))
          (assoc :name (text+ [:h4]))
          (assoc :link (some-> (q+ [:h4 :a])
                               (get-in [:attrs :href])
                               (#(u/full-href provider %))))
          (assoc :image (some-> (q? [:.thumbnails :img])
                                (get-in [:attrs :src])
                                (#(u/full-href provider %))))
          (assoc :type (str (p/category-name provider) " " (spec "Тип:")))
          (assoc :timestamp (u/now))
          (assoc :country (spec "Страны:"))
          (assoc :alcohol (some-> (spec "Крепость:") (u/smart-parse-double)))
          (assoc :volume  (some-> (spec "Обьем:") (u/smart-parse-double)))
          (assoc :available (boolean (q? [:.exists])))
          (assoc :product-code (str (p/pname provider) "_" (text? [:span4 :.code])))
          
          ((fn [doc]
             (let [price (some-> (text? [:.price])
                                 (u/smart-parse-double))
                   oldprice (some-> (text? [:.price-old])
                                    (u/smart-parse-double))
                   sale (boolean oldprice)
                   sale-desc (if sale (format "старая цена %.2f" oldprice))]
               (-> doc
                   (assoc :price price)
                   (assoc :sale sale)
                   (assoc :sale-description sale-desc)))))
          
          ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Alcovegas"
          :base-url      "http://alcovegas.com.ua/"
          :icon          "http://alcovegas.com.ua/uploads/2173/images/logo-alkovegas%20logo1.png"
;;          :icon-width    "263"
;;          :icon-height   "84"
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
                   :threads            1
                   :strategy           :light
                   :node->document     node->document
                   :node-selector      [:.itemlist]
                   :last-page-selector [:.invalid-class] ;; TODO we don't use paging
                   }
   })
