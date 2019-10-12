(ns priceous.provider.alcoland
  (:require [priceous.spider.provider :as p]
            [priceous.spider.selector-utils :as su]
            [priceous.utils.time :as time]
            [priceous.utils.numbers :as numbers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Абсент" "http://alcoland.com.ua/absent/"]
    ["Аперитив" "http://alcoland.com.ua/aperitiv/"]
    ["Бальзам" "http://alcoland.com.ua/balzam/"]
    ["Вермут" "http://alcoland.com.ua/vermut/"]
    ["Вино" "http://alcoland.com.ua/vino/"]
    ["Виски бленд" "http://alcoland.com.ua/viski-kupazhirovannyj/"]
    ["Виски односолодовый" "http://alcoland.com.ua/viski/"]
    ["Водка" "http://alcoland.com.ua/vodka/"]
    ["Джин" "http://alcoland.com.ua/dzhin/"]
    ["Коньяк" "http://alcoland.com.ua/konyak/"]
    ["Ликер" "http://alcoland.com.ua/liker/"]
    ["Пиво" "http://alcoland.com.ua/pivo/"]
    ["Ром" "http://alcoland.com.ua/rom/"]
    ["Текила" "http://alcoland.com.ua/tekila/"]
    ["Чача" "http://alcoland.com.ua/chacha/"]
    ["Шампанское" "http://alcoland.com.ua/shampanskoe/"]]
   (mapv (fn [[name url]] {:name name :template (str url "?page=%s")}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  [provider nodemap]
  (su/with-selectors provider nodemap
    (-> {}
        (assoc :provider (p/pname provider))
        (assoc :name (text+ [:.name]))
        (assoc :link (some-> (q+ [:.name :a]) (get-in [:attrs :href])))
        (assoc :image (some-> (q? [:.image :a :img]) (get-in [:attrs :src])))
        (assoc :type (p/category-name provider))
        (assoc :timestamp (time/now))

        ;; price/sales block
        ((fn [doc]
           (let [priceold (some-> (text? [:.price-old]) (numbers/smart-parse-double))
                 pricenew (some-> (text? [:.price-new]) (numbers/smart-parse-double))
                 price (some-> (text? [:.price]) (numbers/smart-parse-double))
                 sale (boolean priceold)
                 sale-description (if sale (format "старая цена %.2f" priceold))
                 realprice (or pricenew price)]
             
             (-> doc
                 (assoc :sale sale)
                 (assoc :sale-description sale-description)
                 (assoc :price realprice)))))

        (assoc :available (not (re-seq #"Нет в наличии" (text? [:.stock]))))
        )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Alcoland"
          :base-url      "http://alcoland.com.ua/"
          :icon          "http://alcoland.com.ua/image/data/logo.png"
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
                   :node-selector      [:.prod]
                   :last-page-selector [:.pagination #{:a :b}]
                   :last-page-process-fn (fn [node]
                                           (cond (= (:tag node) :a)
                                                 (some->> (get-in node [:attrs :href])
                                                          (re-seq #"page=(\d+)")
                                                          (first)
                                                          (second))
                                                 :else node))
                   }
   })
