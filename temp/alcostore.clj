(ns priceous.provider.alcostore
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->> (su/select*+ (u/fetch (get-in provider [:info :base-url]))
                    provider
                    [:#menu :> :ul :> :li :> :a])
       (map (fn [node]
              {:name (html/text node)
               :template (str (get-in node [:attrs :href]) "?page=%s")}))
       (remove (fn [node] (#{"Новинки" "Продукты" "Главная"} (:name node))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  "Read html resource from URL and transforms it to the document"  
  [provider nodemap]
  (su/with-selectors provider nodemap
    (let [spec (->> [(q*? [:.attribute :tr [:td html/first-child]])
                     (q*? [:.attribute :tr [:td html/last-child]])]
                    (apply mapv (fn [a b] [(-> a html/text u/cleanup)
                                           (-> b html/text u/cleanup)]))
                    (into {}))]
      (-> {}
          (assoc :provider (p/pname provider))
          (assoc :name (text+ [[:h1 (html/attr= :itemprop "name")]]))
          (assoc :link (:link nodemap))
          (assoc :image (some-> (q+ [:.image [:img :#image]])
                                (get-in [:attrs :src])))
          (assoc :country (spec "Регион"))
          (assoc :type (str (p/category-name provider) " " (spec "Тип")))
          (assoc :alcohol (some-> (spec "Крепость") (u/smart-parse-double)))
          (assoc :timestamp (u/now))
          (assoc :volume (some-> (spec "Литраж") (u/smart-parse-double)))

          (assoc :description (text? [:.category1_desc]))
          
          ;; prices
          ((fn [doc]
             (let [price (some-> (text? [:.price-tag])
                                 (u/smart-parse-double))
                   oldprice-all (some->> (q*? [:.price-old])
                                         (map html/text)
                                         (map u/smart-parse-double))
                   oldprice-box (some->> (q*? [:.box :.price-old])
                                         (map html/text)
                                         (map u/smart-parse-double))
                   oldprice (if (and (= (count oldprice-all) (count oldprice-box)))
                              nil
                              (first oldprice-all))
                   available (not (some->> (text? [:.description])
                                          (re-seq #"Нет в наличии")))
                   sale (boolean oldprice)
                   sale-desc (if (and price oldprice (< price oldprice))
                               (format "старая цена %.2f" oldprice))]
               (-> doc
                   (assoc :available (and available (> price 0)))
                   (assoc :sale sale)
                   (assoc :sale-description sale-desc)
                   (assoc :price price)))))
          
          ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   ;; provider specific information
   :info {
          :name          "Alcostore"
          :base-url      "http://alcostore.com.ua/"
          :icon          "http://alcostore.com.ua/image/data/fdsschieschk%20(2).png"
          :icon-width    "195"
          :icon-height   "89"
          }
   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :page-template  :category-managed
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           :current-val    1
           :init-val       1
           :advance-fn     inc
           }

   :configuration {
                   :categories-fn       get-categories
                   :threads             8
                   :strategy            :heavy
                   :node->document      node->document
                   :node-selector       [:.product-list :> :div]
                   :link-selector       [:.image :a]
                   :link-selector-type  :full-href
                   :last-page-selector  [:.pagination :.links #{:a :b}]
               }
   
   })

