(ns priceous.template
  (:require [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [hiccup.page :as page]))

(defn render-item [{:keys [price name provider_name link image]}]
  [:div {:class "item"}
   #_[:div {:class "item-image"}
    [:img {:src image}]]
   [:div {:class "item-name"} [:a {:href link
                                   :target "_blank"} name]]
   
   [:div {:class "item-provider"} provider_name]
   [:div {:class "item-price"} price]
   ])

(defn search [content]
  (page/html5
   [:head 
    ;; TODO
    [:link {:rel "shortcut icon" :href "/images/favicon.ico"}]

    ;; TODO
    (page/include-css "/css/priceous.css")

    ;; TODO
    (page/include-css "https://fonts.googleapis.com/css?family=Alegreya")]
   [:body
    [:div] ;; header 
    [:div
     [:h1 (str (:title content))]

     (form-to
      [:get (str "/search")]
      [:div (text-area 
             {:size 100 :rows 1 :class "query-text-area"}
             "query"
             (:query content))]
      [:div
       [:input
        {:class "submit-query" :type "submit" :value "Search"}]])

     (let [{:keys [status items]} (:items content)]
       (cond
         (= :success status)
         [:div
          [:p "SUCCESS"]
          [:div (for [i items] (render-item i))]]

         (= :error status)
         [:div {:class "error"}
          (str "Invalid query: " (:query content))]

         :else
         [:div {:class "error"}
          (str "Invalid status:" status)]
         
         ))
     
     ]
    [:div] ;; footer
    ]
   ))

(defn admin []
  (page/html5
   [:head 
    ;; TODO
    [:link {:rel "shortcut icon" :href "/images/favicon.ico"}]

    ;; TODO
    (page/include-css "/css/priceous.css")

    ;; TODO
    (page/include-css "https://fonts.googleapis.com/css?family=Alegreya")]
   [:body
    [:div "Admin interface"]
    
    ]))

