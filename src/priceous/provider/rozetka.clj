(ns priceous.provider.rozetka
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]
            [priceous.utils :as u]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->> [["Вино" "http://rozetka.com.ua/vino/c4594285/"]
        ["Другие Крепкие" "http://rozetka.com.ua/krepkie-napitki/c4594292/"]
        ["Ликеры" "http://rozetka.com.ua/liquor-vermouth-syrup/c4625409/"]
        ["Пиво" "http://rozetka.com.ua/pivo/c4626589/"]
        ["Слабоалкогольные напитки" "Http://rozetka.com.ua/slaboalkogoljnye-napitki/c4628313/"]]
       (map (fn [[name template]] {:name name :template (str template "filter/page=%s/")}))
       ))


(defn- parse-price-rozetka-json [s]
  (-> (re-seq #"var pricerawjson = \"(.*)\";" s)
      (first)
      (second)
      (java.net.URLDecoder/decode)
      (json/parse-string true)))

(defn- node->document
  [provider {page :page node :node link :link :as nodemap}]
  (su/with-selectors provider nodemap
    (let [spec (->> [(q* [:.detail-chars-l-i-title])       ;;keys
                     (q* [:.detail-chars-l-i-field])]      ;;values
                    ;; remove glossary icons
                    (map (fn [ks-and-vs]
                           (map (fn [n]
                                  (-> (html/at n [:.glossary-icon] nil)
                                      (first)
                                      (html/text)
                                      (u/cleanup)
                                      ((fn [e]
                                         (if (.endsWith e " ")
                                           (.substring e 0 (dec (count e)))
                                           e)))))
                                ks-and-vs)))
                    ;; group by pairs
                    ((fn [name-and-fields]
                       (apply map (fn [k v] [k v]) name-and-fields)))
                    (into {}))]
      (-> {}
          (assoc :provider (p/pname provider))
          (assoc :name (text+ [:.detail-title]))
          (assoc :link link)
          (assoc :image (-> (q+ [:#detail_image_container :.responsive-img :img])
                            (get-in [:attrs :src])))
          (assoc :country (-> (str (or (spec "Страна") (spec "Страна происхождения")))
                              (str " " (spec "Регион"))
                              (u/cleanup)))
          (assoc :wine_grape (-> (spec "Сорт винограда") (u/cleanup)))
          (assoc :vintage (let [vintage (spec "Год (винтаж)")]
                            (if (= vintage "Не винтажное") nil vintage)))
          
          (assoc :producer (some-> (q*? [:.breadcrumbs-i :.breadcrumbs-title])
                                   (last)
                                   (html/text)
                                   (u/cleanup)))

          (assoc :type (-> (condp = (p/category-name provider) ;; TODO mess extract
                             "Вино" (cond
                                      (= "Игристое" (spec "Категория"))
                                      (str "Вино Игристое " (spec "Содержание сахара"))
                                      :else (str "Вино" " " (spec "Цвет") " " (spec "Содержание сахара")))
                             
                             "Пиво" (str (spec "Категория") " " (spec "Тип") " " (spec "Вид"))
                             (str (spec "Категория")))
                           (u/cleanup)))
          
          (assoc :alcohol (some-> (spec "Крепость, %") (u/smart-parse-double)))
          (assoc :product-code (format "%s_%s"
                                       (p/pname provider)
                                       (text+ [[:span (html/attr= :name "goods_code")]])))
          (assoc :timestamp (u/now))                                            
          (assoc :description (-> (str (spec "Вкус") " " (spec "Аромат"))
                                  (u/cleanup)))
          (assoc :available (some-> (q? [:.detail-available])
                                    (boolean)))
          (assoc :volume (-> (spec "Объем") (u/smart-parse-double)))

          ((fn [p]
             (let [priceblock (q+ [[:div (html/attr= :name "block_desc")] 
                                   [:script (html/pred #(.contains (html/text %) "pricerawjson"))]])
                   old-price (some-> priceblock (html/text)
                                     (parse-price-rozetka-json)
                                     (:old_price)
                                     (u/smart-parse-double))
                   price     (some-> priceblock (html/text)
                                     (parse-price-rozetka-json)
                                     (:price)
                                     (u/smart-parse-double))
                   promoprice (if (= "цена по промокоду" (html/text (q? [:.g-addprice-text-title])))
                                (some-> (text? [:.g-addprice-text-price])
                                        (u/smart-parse-double)))

                   sale-old-price (and price old-price (> old-price price))
                   sale-promo-price (and price promoprice (> price promoprice))
                   sale (or sale-old-price sale-promo-price)
                   sale-description (cond
                                      sale-old-price (format "старая цена %.2f" old-price)
                                      sale-promo-price (format "Цена по промокоду, старая цена %.2f" price)
                                      :else nil)
                   actual-price (if sale-promo-price promoprice price)]
               (-> p
                   (assoc :price actual-price)
                   (assoc :sale sale)
                   (assoc :sale-description sale-description)))))
          
          ;; TODO :item_new

          ))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name "Rozetka"
          :base-url "http://rozetka.com.ua/"
          :icon "http://i1.rozetka.ua/logos/0/99.png"
          :icon-width "134"
          :icon-height "34"
          }
   
   :state {
           :page-current   1
           :page-processed 0
           :page-template  "http://rozetka.com.ua/krepkie-napitki/c4594292/filter/page=%s"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :fetch-strategy :heavy

   :configuration {
                   :categories-fn      get-categories
                   :threads            8
                   :strategy           :heavy
                   :node->document     node->document
                   :node-selector      [:.g-i-tile-i-box-desc]
                   :link-selector      [:.g-i-tile-i-title :a]
                   :link-selector-type :full-href
                   :last-page-selector [[:ul (html/attr= :name "paginator")] :.paginator-catalog-l-link]
                   }
   })
