(ns priceous.provider.etelefon
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Виски" "http://www.e-telefon.com.ua/cat.php?act=showgoods&categoryid=10&limit=%s"]
    ["Виски Бурбон" "http://www.e-telefon.com.ua/cat.php?act=showgoods&categoryid=13&limit=%s"]
    ["Коньяк" "http://www.e-telefon.com.ua/cat.php?act=showgoods&categoryid=12&limit=%s"]
    ["Бренди" "http://www.e-telefon.com.ua/cat.php?act=showgoods&categoryid=24&limit=%s"]
    ["Кальвадос" "http://www.e-telefon.com.ua/cat.php?act=showgoods&categoryid=23&limit=%s"]
    ["Водка" "http://www.e-telefon.com.ua/cat.php?act=showgoods&subcategoryid=37&limit=0"]
    ["Джин" "http://www.e-telefon.com.ua/cat.php?act=showgoods&subcategoryid=38&limit=0"]
    ["Граппа" "http://www.e-telefon.com.ua/cat.php?act=showgoods&subcategoryid=39&limit=0"]
    ["Шампанское" "http://www.e-telefon.com.ua/cat.php?act=showgoods&categoryid=15&limit=%s"]
    ["Ром" "http://www.e-telefon.com.ua/cat.php?act=showgoods&categoryid=19&limit=%s"]
    ["Ликеры и настойки" "http://www.e-telefon.com.ua/cat.php?act=showgoods&categoryid=18&limit=%s"]]
   (mapv (fn [[name url]] {:name name :template url}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  "Read html resource from URL and transforms it to the document"  
  [provider nodemap]
  (su/with-selectors provider nodemap
    (-> {}
        (assoc :provider (p/pname provider))
        (assoc :name (text+ [:.goodtitle])) ;; TODO they still use cp1251 oh god...
        (assoc :link (some-> (q+ [:.goodtitle :a])
                             (get-in [:attrs :href])
                             (#(u/full-href provider %))))
        (assoc :image (some-> (q+ [:.goodbody :img])
                              (get-in [:attrs :src])
                              (#(u/full-href provider %))))
        (assoc :type (p/category-name provider))
        (assoc :timestamp (u/now))
        (assoc :price (some-> (text? [:.goodfooter :b])
                              (u/smart-parse-double)))
        ((fn [doc]
           (if (> (:price doc) 0)
             (assoc doc :available true)
             (-> doc (assoc :price nil) (assoc :available false)))))
        
        )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   ;; provider specific information
   :info {
          :name          "Etelefon"
          :base-url      "http://www.e-telefon.com.ua/"
          :icon          "/images/etelefon.png"
          :icon-width    "500"
          :icon-height   "110"
          }
   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :page-template  :category-managed
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           :current-val    0
           :init-val       0
           :advance-fn     (partial + 30)
           }

   :configuration {
                   :categories-fn       get-categories
                   :threads             2
                   :strategy            :light
                   :node->document      node->document
                   :node-selector       [:table [:table (html/has [:.goodtitle]) (html/attr= :cellspacing "0")]]
                   :last-page-selector  [:center :a]
               }
   
   })
