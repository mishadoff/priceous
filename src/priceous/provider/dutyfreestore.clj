(ns priceous.provider.dutyfreestore
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Виски" "http://duty-free-store.com.ua/catalog/viski"]
    ["Водка" "http://duty-free-store.com.ua/catalog/vodka"]
    ["Вермут" "http://duty-free-store.com.ua/catalog/martini"]
    ["Текила" "http://duty-free-store.com.ua/catalog/tekila"]
    ["Ликер" "http://duty-free-store.com.ua/catalog/likery"]
    ["Самбука" "http://duty-free-store.com.ua/catalog/sambuca"]
    ["Джин" "http://duty-free-store.com.ua/catalog/dzhin"]
    ["Бренди" "http://duty-free-store.com.ua/catalog/brendi"]
    ["Ром" "http://duty-free-store.com.ua/catalog/rom"]
    ["Абсент" "http://duty-free-store.com.ua/catalog/absent"]]
   (mapv (fn [[name url]] {:name name :template url}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  [provider nodemap]
  (su/with-selectors provider nodemap
    (when (:page nodemap)
      (let [spec (->> [(q*? [:.features :li :label])
                       (q*? [:.features :li :span])]
                      (apply mapv (fn [a b] [(-> a html/text u/cleanup)
                                             (-> b html/text u/cleanup)]))
                      (into {}))]
        (-> {}
            (assoc :provider (p/pname provider))
            (assoc :name (-> (text+ [:#page_title [:h1 (html/attr-has :data-product)]])
                             ((fn [name]
                                (let [[orig found] (re-find #"Купить (.*) в Duty Free" name)]
                                  (if found found name))))))
            (assoc :link (:link nodemap))
            (assoc :image (some-> (q? [:.product.page :.image :.wrap :img])
                                  (get-in [:attrs :src])))
            (assoc :type (-> (str (p/category-name provider) " " (spec "Вид виски:"))
                             (u/cleanup)))
            (assoc :timestamp (u/now))
            (assoc :country (spec "Страна производитель:"))
            (assoc :description (spec "Вкус:"))
            (assoc :alcohol (some-> (spec "Алкоголь, градусы:") (u/smart-parse-double)))
            (assoc :volume  (some-> (spec "Объем:") (u/smart-parse-double)))

            (assoc :price (some-> [:.product.page :.price :span]
                                  text?
                                  (u/smart-parse-double)))

            ((fn [doc]
               (assoc doc :available (and (:price doc) (> (:price doc) 0)
                                          (not (some->> (text? [:#tab1])
                                                        (re-seq #"НЕТ В НАЛИЧИИ!|Нет в наличии!"))))))))))))
            


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Dutyfreestore"
          :base-url      "http://duty-free-store.com.ua/"
          :icon          "/images/dutyfreestore.png"}

   
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
           :advance-fn     inc}

   
   :configuration {
                   :categories-fn      get-categories
                   :threads            1  ;; 1 thread here is important server return 500 if page is requested already
                   :strategy           :heavy
                   :node->document     node->document
                   :node-selector      [[:.product (html/but (html/has-class "hover_mouse"))]]
                   :link-selector      [:.image :a]
                   :link-selector-type :relative
                   :link-fixer         (fn [link] (clojure.string/replace link " " "+"))
                   :last-page-selector [:.invalid-class]}}) ;; TODO we don't use paging


