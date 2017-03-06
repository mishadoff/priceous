(ns priceous.provider.alcoland
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Собственный импорт" "http://alcoland.com.ua/sobstvennyj-import/"]
    ["Абсент" "http://alcoland.com.ua/absent/"]
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
        (assoc :timestamp (u/now))
        (assoc :price (some-> (text? [:.price]) (u/smart-parse-double)))
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
