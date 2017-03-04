(ns priceous.provider.kavist
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]
            [priceous.utils :as u]
            [clj-http.client :as http]
            [taoensso.timbre :as log]
            [cheshire.core :as json]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->>
   [["Все напитки" "http://kavist.com.ua/55-vse-napitki#/page-%s/"]]
   (mapv (fn [[name url]] {:name name :template url}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  [provider {page :page node :node link :link :as nodemap}]
  (su/with-selectors provider nodemap
    (when page
      (let [spec (->> (concat (q*? [:#idTab2 :li])
                              (q*? [:#idTab1 :li]))
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
            (assoc :name (text+ [:h1 [:span (html/attr= :itemprop "name")]]))

            (assoc :price (some-> (text+ [:#our_price_display])
                                  (u/smart-parse-double)))
            (assoc :link link)
            (assoc :image (-> (q+ [:#image-block :img])
                              (get-in [:attrs :src])))
            (assoc :country (str (spec "Страна") " " (spec "Регион")))
            (assoc :available true)
            (assoc :volume (-> (spec "Обьем") (u/smart-parse-double)))
            (assoc :alcohol (-> (spec "Крепость") (u/smart-parse-double)))
            (assoc :timestamp (u/now))
            (assoc :description (some->> (list (spec "Аромат") (spec "Вкус") (spec "Послевкусие"))
                                         (remove empty?)
                                         (str/join ";")))
            (assoc :type (some->> (list (spec "Тип напитка")
                                        (spec "Тип виски")
                                        (spec "Цвет шампанского")
                                        (spec "Сахар"))
                                  (remove empty?)
                                  (str/join " ")
                                  (u/cleanup)))
            (assoc :producer (some-> (su/select*? node provider [:.manufacturer_pr])
                                     (html/text)
                                     (u/cleanup)))
            ;; TODO new, sale, sale-description


            ;; post-filter
            ((fn [doc] (if (= "Товар отсутствует" (:name doc)) {} doc)))
            )))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

                   ;; :link-selector      [:.product_img_link :img]
                   ;; :link-selector-type :full-href
                   ;; :last-page-selector [:.pagination :li]


(defn fetch-page-fn [provider]
  (let [api-url (format (get-in provider [:custom :api-template])
                        (get-in provider [:state :current-val]))
        result (http/get api-url)]
    (if (not= 200 (:status result))
      (do (log/error (format "Problem sending request, status %s" (:status result)))
          "")
      (-> (json/parse-string (:body result) true)
          ((fn [raw]
             (concat (html/html-snippet (:productList raw))
                     (html/html-snippet (:pagination raw)))))))))
 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Kavist"
          :base-url      "http://kavist.com.ua/"
          :icon          "http://kavist.com.ua/img/logo.jpg"
          :icon-width    "156"
          :icon-height   "72"
          }
   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :page-template  "http://kavist.com.ua/55-vse-napitki#/page-%s/"
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           
           :current-val    1
           :init-val       1
           :advance-fn     inc

           }
   
   :configuration {
                   :categories-fn      get-categories
                   :threads            8
                   :strategy           :heavy
                   :fetch-page-fn      fetch-page-fn
                   :node->document     node->document
                   :node-selector      [:.ajax_block_product]
                   :link-selector      [:.product_img_link]
                   :link-selector-type :full-href
                   :last-page-selector [:.pagination :li]
                   }
   :custom {
            :api-template "http://kavist.com.ua/modules/blocklayered/blocklayered-ajax.php?id_category_layered=55&orderby=popularity&orderway=asc&n=12&p=%s"
            }
   })
