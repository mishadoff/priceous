(ns priceous.templates.search
  (:require [clj-time.format :as tf]
            [clojure.string :as str]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [hiccup.page :as page]
            [priceous.templates.base :as base]
            [priceous.config :as config]
            [priceous.utils :as u]
            [taoensso.timbre :as log]))

(declare 
 view
 search-container
 render-item
 status-bar
 search-input
 query-example
 )

(u/require-all-providers)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn view [response] (base/page search-container response))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn search-container [content]
  [:div {:class "full-container"}
   [:div {:class "search-left-panel"}
    [:p {:class "query-examples-title"} "Примеры запросов:"]
    [:ul {:class "query-examples"}
     (query-example "Миниатюры (0.05)" "/search?query=0.05")
     (query-example "Glenfiddich 12yo 0.7" "/search?query=glenfiddich+12+0.7")
     (query-example "Односолодовый виски" "/search?query=односолодовый")
     (query-example "Бленды" "/search?query=бленд")
     (query-example "Бурбоны" "/search?query=бурбон")
     (query-example "Страна: Шотландия" "/search?query=шотландия")
     (query-example "Только Goodwine" "/search?query=Goodwine")
     ]
    ]

   [:div {:class "search-right-panel"}
    (search-input content)
    (status-bar content)

    [:div {:class "search-container"}
     (let [{:keys [status data]} (:response content)]
       (cond
         (or (= :success status) (empty? (:query content)))
         [:div
          [:div (for [i (get-in data [:response :docs])] (render-item i))]]))]]

   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-item [item]
  [:div {:class "item"}

   ;; IMAGE
   [:div {:class "item-images-container"}
    [:div {:class "item-image"} [:img {:src (:image item)}]]]

   ;; NAME AND LINK
   [:div {:class "item-main-container"}
    [:div {:class "item-main-top"}
     [:span {:class "item-name"}
      [:a {:href (:link item)
           :target "_blank"
           :class "itemlink"} (:name item)]]]
    
    ;; PRICE
    [:div {:class "item-main-bottom"}
     [:div {:class "item-price"}
      (let [price (:price item)]
        (cond
          price (let [[grn kop] (u/split-price price)]
                  [:div
                   [:span {:class "grn"} grn]
                   [:span {:class "kop"} kop]])
          
          ;; if price not available
          :else [:div {:class "price-na"} "Цены нет"]))]
     
     ;; PROVIDER / SHOP
     ;; TODO memoize provider resolution
     (let [p (u/resolve-provider-by-name (.toLowerCase (:provider item)))]
       [:div {:class "provider-element"}
        [:a {:href (get-in p [:info :base-url])}
         [:img {:src (get-in p [:info :icon])
                :title (:provider item)
                :alt (:provider item)
                :width (get-in p [:info :icon-width] "70")
                :height (get-in p [:info :icon-height] "34")}]]])
    
     (let [{:keys [sale sale_description]} item]
       (if sale
         [:div {:class "sale-description"}
          (format "Акция: %s" sale_description)]))
     
     ]
    ]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn status-bar [content]
  (cond
    (= :error (get-in content [:response :status]))
    [:div {:class "status-bar-error"} "Ошибка: Неправильный запрос"]
    
    ;; Query not entered, first time here
    (empty? (:query content))
    [:div {:class "status-bar-small"}
     "Введите запрос, например "

     ;; TODO make this generatable
     [:a {:href "/search?query=springbank" :class "amber"}
      "springbank"]]

    ;; Query entered, but nothing found
    (and (not (empty? (:query content)))
         (zero? (get-in content [:response :data :response :numFound])))
    [:div {:class "status-bar-regular"}
     "Ничего не найдено."]
    
    :else
    [:div {:class "status-bar-small"}
     (format "Найдено %s товаров за %s секунд. "
             (get-in content [:response :data :response :numFound])
             (some-> (get-in content [:response :data :responseHeader :QTime])
                     (/ 1000.0)))
     (if (> (get-in content [:response :data :response :numFound] 0)
            (count (get-in content [:response :data :response :docs] [])))
       [:span {:class "status-bar-warning"}
        "(показано только первых 50 товаров)"])
     
     ]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  
(defn- search-input [content]
  (form-to
   [:get (str "/search")]
   [:div 
    [:div {:class "query-container"}
     (text-field
      {:size 50 :rows 1 :class "query-text-field"}
      "query"
      (:query content))]]
    [:div
     [:input {:id "search_submit" :type "submit" :value "Search" :tabindex "-1"}]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query-example [name href]
  [:li [:a {:class "amber" :href href} name]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
