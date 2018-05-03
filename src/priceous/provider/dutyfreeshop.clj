(ns priceous.provider.dutyfreeshop
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Виски" "http://dutyfreeshop.com.ua/produktsiya/viski"]
    ["Водка" "http://dutyfreeshop.com.ua/produktsiya/vodka"]
    ["Бренди" "http://dutyfreeshop.com.ua/produktsiya/brendi"]
    ["Джин" "http://dutyfreeshop.com.ua/produktsiya/dzhin"]
    ["Кальвадос" "http://dutyfreeshop.com.ua/produktsiya/kalvados"]
    ["Текила" "http://dutyfreeshop.com.ua/produktsiya/tekila"]
    ["Ром" "http://dutyfreeshop.com.ua/produktsiya/rom"]
    ["Абсент" "http://dutyfreeshop.com.ua/produktsiya/absent"]
    ["Вермут" "http://dutyfreeshop.com.ua/produktsiya/vermut"]
    ["Ликер" "http://dutyfreeshop.com.ua/produktsiya/likeri-i-nastoiki"]

    ;; cognac categories
    ["Коньяк" "http://dutyfreeshop.com.ua/produktsiya/ordinarnye-konyaki"]
    ["Коньяк" "http://dutyfreeshop.com.ua/produktsiya/marochnye-konyaki"]
    ["Коньяк" "http://dutyfreeshop.com.ua/produktsiya/kollektsionnye-konyaki"]
    ["Коньяк" "http://dutyfreeshop.com.ua/produktsiya/elitnye-konyaki"]]

    ;; TODO no wine from this site (just moldova not interesting)

   (mapv (fn [[name url]] {:name name :template (str url "/?start=%s")}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  [provider nodemap]
  (su/with-selectors provider nodemap
    (when (:page nodemap)
      (let [spec (->> (q*? [:.b1-info])
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
            
            (assoc :name (-> (text+ [:h1])
                             ((fn [name]
                                (let [[orig found] (re-find #"(.*)\(Код:" name)]
                                  (if found found name))))))
            (assoc :link (:link nodemap))
            (assoc :image (some-> (q? [:#list_product_image_middle :img])
                                  (get-in [:attrs :src])))            
            (assoc :type (p/category-name provider))
            (assoc :timestamp (u/now))
            
            
            #_(assoc :country (spec "Страна производитель:"))
            #_(assoc :description (spec "Вкус:"))
            (assoc :alcohol (some-> (spec "Крепость") (u/smart-parse-double)))
            (assoc :volume  (some-> (spec "Ёмкость") (u/smart-parse-double)))

            ((fn [doc]
               (let [price (some-> (text? [:#block_price]) (u/smart-parse-double))
                     oldprice (some-> (text? [:#old_price]) (u/smart-parse-double))
                     available (pos? price)]
                 (-> doc
                     (assoc :available available)
                     (assoc :price (u/force-pos price))
                     (assoc :sale (boolean oldprice))
                     (assoc :sale_description (if oldprice (format "старая цена %.2f" oldprice) nil)))))))))))

            


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Dutyfreeshop"
          :base-url      "http://dutyfreeshop.com.ua/"
          :icon          "/images/dutyfreeshop.png"}

   
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
           :advance-fn     (partial + 12)}

   
   :configuration {
                   :categories-fn      get-categories
                   :threads            4
                   :strategy           :heavy
                   :node->document     node->document
                   :node-selector      [:.block_product]
                   :link-selector      [:.name :a]
                   :link-selector-type :relative
                   :last-page-selector [:.pagination :li :span]}})


