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
     (footer content)]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header [content]
  [:div {:class "header"}
   [:div {:class "header_icon"}
    [:a {:href "/search" :class "amber h100"}
     [:div {:class "header_whisky_block"}
      [:div {:class "header_whisky_block_text"} [:div "Whisky"] [:div "Search"]]
      [:div {:class "header_whisky_block_image"}
       [:img {:src "/images/glencairn.png" :width "30" :height "46"}]]
      ]
     ]]

   ;; link to help page
   [:div {:class "header_link"}
    [:a {:href "/help" :class "amber h100"} "Помощь"]]

   ;; stats
   [:div {:class "header_link"}
    [:a {:href "/stats" :class "amber h100"} "Статистика"]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn footer [content]
  (let [version     (config/prop [:app :version])
        build-date  (config/prop [:meta :build-date] (u/now))
        site-name   (config/prop [:app :name])]
    [:div {:class "footer"}
     [:div {:class "footer-left"}
      [:div {:class "footer-name"} site-name]
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
