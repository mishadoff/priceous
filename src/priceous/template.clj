(ns priceous.template
  (:require [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [hiccup.page :as page]))

(defn- provider-element [content]
  [:div {:class "provider_element"}
   [:a {:href (:provider_base_url content)}
    [:img {:src (:provider_icon content)
           :title (:provider_name content)
           :alt (:provider_name content)
           :width "50" :height "30"}]]])

(defn render-item
  "Renders one block of item"
  [{:keys [price name provider_name link image] :as provider}]
  [:div {:class "item"}

   [:div {:class "item-images-container"}
    [:div {:class "item-image"} [:img {:src image}]]]

   [:div {:class "item-main-container"}
    [:div {:class "item-main-top"}
     [:span {:class "item-name"} [:a {:href link :target "_blank"} name]]
     [:span {:class "item-price"} price]]
    
    [:div {:class "item-main-bottom"}
     (provider-element provider)]]

   ])

(defn search-input [content]
  (form-to
   [:get (str "/search")]
   [:div 
    [:div {:class "query-container"}
     (text-field
      {:size 30 :rows 1 :class "query-text-field"}
      "query"
      (:query content))]]
    [:div
     [:input {:id "search_submit" :type "submit" :value "Search" :tabindex "-1"}]]))

(defn- header [content]
  [:div {:class "header"}

   ;; header icon
   [:div {:class "header_icon"}
    [:a {:href "/search"}
     "Whisky Search"]] ;; TODO: Icon an reference to the /search

   ;; link to help page
   [:div {:class "header_link"}
    [:a {:href "/help"} "Help"]]

   (search-input content)

   ])

(defn- footer [content]
  [:div {:class "footer"}])


(defn- search-container [content]
  [:div {:class "container"}
;;   (search-input content)
     (let [{:keys [status items]} (:items content)]
       (cond
         (= :success status)
         [:div
          ;;[:p "SUCCESS"]
          [:div (for [i items] (render-item i))]]

         (= :error status)
         [:div {:class "error"}
          (str "Invalid query: " (:query content))]

         :else
         [:div {:class "error"}
          (str "Invalid status:" status)]
         
         ))
     
     ])


(defn search
  "Basic template for searching"
  [content]
  (page/html5
   [:head 
    ;; TODO: provide icon
    [:link {:rel "shortcut icon" :href "/images/favicon.ico"}]
    ;; resources
    (page/include-css "/css/priceous.css")
    (page/include-css "https://fonts.googleapis.com/css?family=Alegreya")]
   [:body 
    (header content)
    (search-container content)
    (footer content)]))
