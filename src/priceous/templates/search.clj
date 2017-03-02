(ns priceous.templates.search
  (:require [clj-time.format :as tf]
            [clojure.string :as str]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [hiccup.page :as page]
            [priceous.templates.base :as base]
            [priceous.templates.query-examples :as qe]
            [priceous.config :as config]
            [priceous.utils :as u]
            [taoensso.timbre :as log]))

(declare 
 view
 search-container
 render-item
 status-bar
 search-input
 pagination
 sorting)

(u/require-all-providers)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn view [response] (base/page search-container response {:title "Priceous"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn search-container [content]
  [:div
   [:div {:class "search-left-panel"}
    [:p {:class "query-examples-title"} "Примеры запросов:"]
    [:ul {:class "query-examples"}
     (for [[name href] qe/queries]
       [:li (qe/query-example name href)])]]

   [:div {:class "search-right-panel"}
    (search-input content)
    (status-bar content)

    [:div {:class "search-controls"}
     [:div {:class "pagination-parent"}
      (pagination content)]
     [:div {:clsss "sorting-parent"}
      (sorting content)]]
    
    [:div {:class "search-container"}
     (let [{:keys [status data]} (get-in content [:solr :response])]
       (cond
         (or (= :success status) (empty? (get-in content [:params :query])))
         [:div
          [:div (for [i (get-in data [:response :docs])] (render-item i))]]))]


    [:div {:class "search-controls"}
     [:div {:class "pagination-parent"}
      (pagination content)]]
    

    ]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-item [item]
  [:div {:class "item"}
   [:div {:class "item-container"}
    [:div {:class "item-left"}
     [:div {:class "item-image-container"}
      [:a {:href (:link item) :target "_blank" :class "itemlink"}
      [:img {:class "nested_fixed_img" :src (:image item)}]
       ]]
     ]

    [:div {:class "item-right"}
     [:div {:class "item-price"}
      (let [price (:price item)]
        (cond
          price (let [[grn kop] (u/split-price price)]
                  [:div
                   [:span {:class "grn"} grn]
                   [:span {:class "kop"} kop]])
          
          ;; if price not available
          :else [:div {:class "price-na"} "Цены нет"]))]

     ;; provider
     [:div {:class "item-provider"}
      (let [p (u/resolve-provider-by-name (.toLowerCase (:provider item)))]
        [:a {:href (get-in p [:info :base-url])}
         [:img {:class "nested_fixed_img"
                :src (get-in p [:info :icon])
                :title (:provider item)
                :alt (:provider item)}]])]

     ]

    
    [:div {:class "item-center"}
     [:div {:class "item-name"}
      [:a {:href (:link item)
           :target "_blank"
           :class "itemlink"} (:name item)]]

     ;; alcohol
     (if (:alcohol item)
       [:div {:class "itemprop"}
        (format "Крепость: %s%%" (u/format-decimal-up-to-2 (:alcohol item)))])

     (if (:type item)
       [:div {:class "itemprop"}
        (format "Тип: %s" (:type item))])

     (if (:country item)
       [:div {:class "itemprop"}
        (format "Регион: %s" (:country item))])

     ;; only for wines
     (if (:wine_grape item)
       [:div {:class "itemprop"}
        (format "Сорт: %s" (:wine_grape item))])
     
     (if (:wine_sugar item)
       [:div {:class "itemprop"}
        (format "Сахар: %s г/л" (:wine_sugar item))])
     
     (if (:sale item)
       [:div {:class "sale"}
        (format "Акция: %s" (:sale_description item))])


     ]

    ]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn status-bar [content]
  (cond
    (= :error (get-in content [:solr :response :status]))
    [:div {:class "status-bar-error"} "Ошибка: Неправильный запрос"]
    
    ;; Query not entered, first time here
    (empty? (get-in content [:params :query]))
    [:div {:class "status-bar-small"}
     "Введите запрос, например "
     
     (let [[name href] (rand-nth qe/queries)]
       [:a {:href href :class "link"} name])]
    
    ;; Query entered, but nothing found
    (and (not (get-in content [:params :query]))
         (zero? (get-in content [:solr :response :data :response :numFound])))
    [:div {:class "status-bar-regular"}
     "Ничего не найдено."]
    
    :else
    [:div 
     [:div {:class "status-bar-small"}
      (format "Найдено %s товаров за %s секунд. "
              (get-in content [:solr :response :data :response :numFound])
              (some-> (get-in content [:solr :response :data :responseHeader :QTime])
                      (/ 1000.0)))]
     

      ;; TODO build links based for this use query DO not use page
      ;; TODO ALL from query params
     
      ]

     ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  
(defn- search-input [content]
  (form-to
   [:get (str "/search")]
   [:div 
    [:div {:class "query-container"}
     (text-field
      {:size 50 :rows 1 :class "query-text-field"}
      "query"
      (get-in content [:params :query]))]]
    [:div
     [:input {:id "search_submit" :type "submit" :value "Search" :tabindex "-1"}]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- pagination [content]
  (when (not (empty? (get-in content [:params :query])))
    (let [numfound (get-in content [:solr :response :data :response :numFound])
          perpage (config/prop [:view :per-page] 10)
          current-page ;; TODO extract
          (or (try (Integer/parseInt (get-in content [:params :page]))
                   (catch NumberFormatException e nil)) 1)
          pages (int (Math/ceil (/ numfound perpage)))
          page-context ;; TODO extract
          (->> (into [] (range (max (- current-page 2) 1)  (inc (min pages (+ current-page 2)))))
               ((fn [ctx]
                  (cond
                    (empty? ctx) ctx
                    (= (first ctx) 1) ctx
                    (> (first ctx) 2) (into [1 :..] ctx)
                    :else (-> []
                              (into (range 1 (first ctx)))
                              (into ctx)))))
               ((fn [ctx]
                  (cond
                    (empty? ctx) ctx
                    (= last pages) ctx
                    (<= (last ctx) (- pages 2)) (into ctx [:.. pages])
                    :else (into ctx (range (inc (last ctx)) (inc pages)))))))]
      ;; TODO if one page do not show pagination

      (when (and (> pages 1) (not (empty? page-context)))
        [:div {:class "pagination"}
         (for [page-index page-context]
           (cond (= current-page page-index)
                 [:span {:class "pagination_current_page"} page-index]
                 (= page-index :..) [:span {:class "pagination-dots"} ".."]
                 :else
                 [:a {:href (format "/search?query=%s&sort=%s&page=%d" 
                                    (java.net.URLEncoder/encode (get-in content [:params :query]))
                                    (or (get-in content [:params :sort]) "cheap") page-index)
                      :class "link"} page-index]))]))))

(defn- sorting [content]
  ;; TODO iterate on all params /except page
  (when (not (empty? (get-in content [:params :query])))
    (let [q (get-in content [:params :query])
          cursort (get #{"cheap" "expensive" "relevant"}
                       (get-in content [:params :sort]) "cheap")]
      [:div {:class "sorting"}
       (for [[s label] [["cheap" "Дешевые"] ["expensive" "Дорогие"]
                        #_["relevant" "Релевантные"]]]
         (if (= s cursort)
           [:span {:class "sorting-current"} label]
           [:a {:href (format "/search?query=%s&sort=%s" (java.net.URLEncoder/encode q) s)
                :class "link"}
            label]))
       ])))
