(ns priceous.templates.base
  (:require [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [hiccup.page :as page]
            [priceous.config :as config]
            [priceous.utils :as u]
            [taoensso.timbre :as log]))

(declare page header footer)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [container-fn content]
  (page/html5
   [:head [:link {:rel "shortcut icon" :href "/images/favicon.ico"}]
    (page/include-css "/css/priceous.css")
    (page/include-css "https://fonts.googleapis.com/css?family='Noto+Sans'")]
   [:body
    [:div {:id "main"}
     (header content)
     (container-fn content)
     #_(footer content)]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header [content]
  [:div {:class "header"}
   [:div 
    [:a {:href "/search" :class "h100"}
     [:img {:src "/images/priceous_logo_2x.png" :width "150" :height "75"}]]]
   
   [:div {:class "mp-root"} 
    [:div {:class "mp-child"}
     [:a {:href "/about"  :class "link h20"} "О проекте"]]]

   [:div {:class "mp-root"} 
    [:div {:class "mp-child"}
     [:a {:href "/help"  :class "link h20"} "Помощь"]]]

   [:div {:class "mp-root"} 
    [:div {:class "mp-child"}
     [:a {:href "/stats" :class "link h20"} "Статистика"]]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn footer [content]
  (let [version     (config/prop [:app :version])
        build-date  (config/prop [:meta :build-date] (u/now))
        site-name   (config/prop [:app :name])]
    [:div {:class "footer"}
     [:div {:class "footer-left"}
      [:div {:class "footer-name"} "priceous"]
      [:div {:class "footer-copyright"} "©"]
      [:div {:class "footer-author"}
       [:a {:href "http://mishadoff.com"
            :class "amber"
            :target "_blank"} "mishadoff.com"]]]

     [:div {:class "footer-center"}]

     [:div {:class "footer-right"
            :align "right"}
      [:div {:class "footer-right-content"}
       (format "Version %s" version)]]]))
