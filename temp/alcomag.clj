(ns priceous.provider.alcomag
  (:require [net.cgrand.enlive-html :as html]
            [priceous.spider.provider :as p]
            [priceous.spider.selector-utils :as su]
            [priceous.utils.http :as http]
            [priceous.utils.time :as time]
            [priceous.utils.collections :as collections]
            [priceous.utils.numbers :as numbers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->> [["Вино белое" "http://alcomag.com.ua/vino/tikhoe/beloe/"]
        ["Вино розовое" "http://alcomag.com.ua/vino/tikhoe/rozovoe/"]
        ["Вино красное" "http://alcomag.com.ua/vino/tikhoe/krasnoe/"]
        ["Вино игристое" "http://alcomag.com.ua/vino/igristoe/"]
        ["Шампанское" "http://alcomag.com.ua/vino/shampanskoe/"]
        ["Бренди" "http://alcomag.com.ua/krepkie-napitki/brendi/"]
        ["Виски" "http://alcomag.com.ua/krepkie-napitki/viski/"]
        ["Водка" "http://alcomag.com.ua/krepkie-napitki/vodka/"]
        ["Джин" "http://alcomag.com.ua/krepkie-napitki/dzhin/"]
        ["Кальвадос" "http://alcomag.com.ua/krepkie-napitki/kalvados/"]
        ["Коньяк" "http://alcomag.com.ua/krepkie-napitki/konyak/"]
        ["Ром" "http://alcomag.com.ua/krepkie-napitki/rom/"]
        ["Текила" "http://alcomag.com.ua/krepkie-napitki/tekila/"]
        ["Граппа" "http://alcomag.com.ua/krepkie-napitki/grappa/"]
        ["Вермут" "http://alcomag.com.ua/likery-vermuty/vermut/"]
        ["Ликер" "http://alcomag.com.ua/likery-vermuty/liker/"]
        ["Подарочные наборы" "http://alcomag.com.ua/podarochnie_nabori/"]]

       (mapv (fn [[name url]] {:name name :template (str url "?PAGEN_1=%s")}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  [provider {page :page node :node link :link :as nodemap}]
  (su/with-selectors provider nodemap
    (when node
      (let [];; spec (->> (q*? [:table.table :tr])
            ;;           (map html/text)
            ;;           (map (fn [s]
            ;;                  (some->> (.split s ":" 2)
            ;;                           (seq)
            ;;                           (map u/cleanup)
            ;;                           (into []))))
            ;;           (filter (fn [v] (= (count v) 2)))
            ;;           (into {}))

        (-> {}
            (assoc :provider (p/pname provider))
            (assoc :name (text+ [:.product-item__title]))
            (assoc :link (some-> (q+ [:.product-item__title :a])
                                 (get-in [:attrs :href])
                                 (#(http/full-href provider %))))
            (assoc :image (some-> (q? [:.bx_catalog_item_images])
                                  (get-in [:attrs :style])
                                  ((fn [style]
                                     (second (re-find #"url\('(.*)'\)" style))))
                                  (#(http/full-href provider %))))
            
            #_(assoc :producer (spec "Производитель"))
            #_(assoc :country (spec "Страна"))
            #_(assoc :alcohol (some-> (spec "Крепость") (u/smart-parse-double)))
            #_(assoc :volume  (some-> (spec "Объем") (u/smart-parse-double)))
            #_(assoc :vintage (spec "Год"))
            #_(assoc :type (-> (str (p/category-name provider) " "
                                  (spec "Содержание сахара")
                                  (spec "Тип"))
                               (u/cleanup)))
            (assoc :type (p/category-name provider))
            (assoc :timestamp (time/now))
            #_(assoc :product-code (str (get-in provider [:info :name])
                                      "_"
                                      (text+ [:.articul :b])))
            ;; assume everything available for now
            (assoc :available true)

            (assoc :price (some-> (q+ [:.bx_price])
                                  (html/at [:span] nil)
                                  (first)
                                  (html/text)
                                  (collections/cleanup)
                                  (numbers/smart-parse-double)))
            ((fn [doc]
               (let [oldprice (some-> (text? [:.bx_price [:span (html/but (html/has-class "snt-catalog-currency"))]])
                                      (numbers/smart-parse-double))
                     sale (boolean oldprice)
                     sale-desc (if sale (format "старая цена %.2f" oldprice) nil)]
                 (-> doc
                     (assoc :sale sale)
                     (assoc :sale-description sale-desc))))))))))
            
            


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name "Alcomag"
          :base-url "http://alcomag.com.ua/"
          :icon "http://alcomag.com.ua/bitrix/templates/snt_alcomag/images/logo.png"}

   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :category       :no-category
           :page-template  :category-managed
           :page-limit     Integer/MAX_VALUE
           :done           false
           :current-val    1
           :init-val       1
           :advance-fn     inc}

   
   :configuration {
                   :categories-fn      get-categories
                   :threads            1
                   :strategy           :light
                   :node->document     node->document
                   :node-selector      [:.bx_catalog_item_container]
                   ;; :link-selector      [:.product-item__title :a]
                   ;; :link-selector-type :relative
                   :last-page-selector [:.ax-pagination-container :ul :li]
                   ;; TODO alcomag very aggressive about crawling, do wait periods
                   :fetch-page-fn      (fn [provider]
                                         (Thread/sleep 1000)
                                         (http/fetch (p/current-page provider)))}})



