(ns priceous.provider.winetime
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]
            [priceous.utils :as u]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-categories [provider]
  (->> [["Крепкие" "http://winetime.com.ua/ru/alcohol/page/%s.htm?size=30"]
        ["Вино" "http://winetime.com.ua/ru/wine/page/%s.htm?size=30"]]
       (mapv (fn [[name url]] {:name name :template url}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn node->document
  [provider {page :page node :node link :link :as nodemap}]
  (su/with-selectors provider nodemap
    (let [spec (su/spec-with-split ":"
                 (concat (q*? [:.harakter_tovar :p]) (q*? [:table.details_about :p])))]
      (-> {}
          (assoc :provider (p/pname provider))
          (assoc :excise true)
          (assoc :trusted true)
          (assoc :name (text+ [:.product-details-wraper :h1]))
          (assoc :link link)
          (assoc :image (some-> (q? [:.foto_main :a :img])
                                (get-in [:attrs :src])
                                (#(u/full-href provider %))))
          (assoc :country (u/cat-items (spec "Страна") (spec "Регион")))
          (assoc :wine_grape (spec "Сорт винограда"))
          (assoc :wine_sugar (u/smart-parse-double (spec "Сахар")))
          (assoc :vintage (spec "Год"))
          (assoc :producer (spec "Производитель"))

          ;; type wine/not wine
          ((fn [doc]
             (cond
               ;; sparkling wine
               (and (= "Вино" (p/category-name provider)) (= "игристое" (spec "Тип")))
               (assoc doc :type (u/cat-items "Вино" (spec "Цвет") (spec "Тип") (spec "Сладость")))

               ;; regular wine
               (= "Вино" (p/category-name provider))
               (assoc doc :type (u/cat-items "Вино" (spec "Цвет") (spec "Сладость")))

               :else
               (assoc doc :type (u/cat-items (spec "Тип") (spec "Классификация"))))))

          (assoc :alcohol (u/smart-parse-double (spec "Алкоголь")))
          (assoc :description (->> (list (spec "Дегустации") (spec "Аромат") (spec "Вкус"))
                                   (remove nil?)
                                   (str/join "; ")))
          (assoc :timestamp (u/now))
          (assoc :product-code (str (get-in provider [:info :name])
                                    "_"
                                    (text+ [:.articul :span])))
          (assoc :available (-> (q? [:.buying_block_do])
                                (boolean)))
          (assoc :item_new (-> (q? [:.foto_main :.badge-new]) (boolean)))
          (assoc :volume (or
                           (u/smart-parse-double (text+ [:.product-details_info-block :.size]))
                           (u/smart-parse-double (spec "Объём"))))

          ;; price only if
          ((fn [doc]
             (if (:available doc)
               (assoc doc :price (some-> (text+ [:.show_all_sum])
                                         (u/smart-parse-double)
                                         (/ 100.0)))
               (assoc doc :price nil))))

          ;; TODO 8% sale

          ((fn [p]
             (let [oldprice (some-> (q*? [:.buying_block_compare :span])
                                    (first)
                                    (html/text)
                                    (u/smart-parse-double))]
               (if oldprice (-> p
                                (assoc :sale true)
                                (assoc :sale-description (format "старая цена %.2f" oldprice)))
                   (-> p (assoc :sale false))))))
          ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name "Winetime"
          :base-url "http://winetime.com.ua"
          :icon "http://winetime.com.ua/shared/site/images/logo_03.jpg"
          :icon-width "119"
          :icon-height "34"
          }

   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :category :no-category
           :page-template "http://winetime.com.ua/alcohol/?size=10000"
           :page-limit     Integer/MAX_VALUE
           :done           false

           ;; for paging (across category)
           :current-val    0

           ;; won't change
           :init-val       0
           :advance-fn     (partial + 30)
           }

   :configuration {
                   :categories-fn      get-categories
                   :threads            8
                   :strategy           :heavy
                   :node->document     node->document
                   :node-selector      [:.item-block_main]
                   :link-selector      [:.item-block-head_main :a]
                   :link-selector-type :relative
                   :last-page-selector [:.pagination :.pag]
                   }
   })

