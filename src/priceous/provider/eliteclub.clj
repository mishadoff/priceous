(ns priceous.provider.eliteclub
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Виски" "http://kiev.eliteclub.com.ua/index.php?id_category=7&controller=category&id_lang=7"]
    ["Бренди" "http://kiev.eliteclub.com.ua/index.php?id_category=139&controller=category&id_lang=7"]
    ["Вермут" "http://kiev.eliteclub.com.ua/index.php?id_category=5&controller=category&id_lang=7"]
    ["Вино" "http://kiev.eliteclub.com.ua/index.php?id_category=6&controller=category&id_lang=7"]
    ["Вино игристое" "http://kiev.eliteclub.com.ua/index.php?id_category=138&controller=category&id_lang=7"]
    ["Водка" "http://kiev.eliteclub.com.ua/index.php?id_category=8&controller=category&id_lang=7"]
    ["Джин" "http://kiev.eliteclub.com.ua/index.php?id_category=9&controller=category&id_lang=7"]
    ["Коньяк" "http://kiev.eliteclub.com.ua/index.php?id_category=10&controller=category&id_lang=7"]
    ["Ликер" "http://kiev.eliteclub.com.ua/index.php?id_category=11&controller=category&id_lang=7"]
    ["Настойка" "http://kiev.eliteclub.com.ua/index.php?id_category=12&controller=category&id_lang=7"]
    ["Пиво" "http://kiev.eliteclub.com.ua/index.php?id_category=13&controller=category&id_lang=7"]
    ["Ром" "http://kiev.eliteclub.com.ua/index.php?id_category=14&controller=category&id_lang=7"]
    ["Текила" "http://kiev.eliteclub.com.ua/index.php?id_category=15&controller=category&id_lang=7"]
    ["Шампанское" "http://kiev.eliteclub.com.ua/index.php?id_category=16&controller=category&id_lang=7"]
    ]
   (mapv (fn [[name url]] {:name name :template (str url "/page-%s")}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  [provider nodemap]
  (su/with-selectors provider nodemap
    (let [;; FIXME NO SPEC because we moved to light strategy
            ;; spec (->> (q*? [:#idTab2 :li])
            ;;           (map (fn [node]
            ;;                  [(some-> (html/select node [:span])
            ;;                           (first)
            ;;                           (html/text)
            ;;                           (u/cleanup))
            ;;                   (some-> (html/at node [:span] nil)
            ;;                           (first)
            ;;                           (html/text)
            ;;                           (u/cleanup))]))
            ;;           (into {}))
            ]
        (-> {}
            (assoc :provider (p/pname provider))
            (assoc :name (text+ [:.bottom_block :h3]))
            (assoc :link (some-> (q+ [:.bottom_block :h3 :a])
                                 (get-in [:attrs :href])))
            (assoc :image (some-> (q? [:.prod_image :img])
                                  (get-in [:attrs :src])))
            
            #_(assoc :product-code (q? [:#product_reference :span]))
            (assoc :type (p/category-name provider))
            #_(assoc :wine_grape (spec "Сорт винограду"))
            #_(assoc :description (text+ [:#short_description_content]))
            (assoc :timestamp (u/now))
            #_(assoc :country (spec "Країна виробник"))
            #_(assoc :producer (spec "Бренд"))
            #_(assoc :alcohol (some-> (spec "Міцність") (u/smart-parse-double)))
            #_(assoc :volume  (some-> (spec "Об'єм") (u/smart-parse-double)))
            (assoc :price (some-> (text? [:.price])
                                  (u/smart-parse-double)))
            (assoc :available (boolean (q? [:.cart_button :a])))
            
            ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Eliteclub"
          :base-url      "http://kiev.eliteclub.com.ua/"
          :icon          "/images/eliteclub.png"
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
                   :threads            1 ;; reduced thread size to prevent server 500
                   :strategy           :light
                   :node->document     node->document
                   :node-selector      [:#grid-view :.ajax_block_product]
                   :last-page-selector [:#pagination :li]
                   }
   ;; TODO this provider throws error if parallel fetches encountered, move to light strategy?
   })
