(ns priceous.web.templates.base
  (:require [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [hiccup.page :as page]
            [trptcolin.versioneer.core :as v]))

(declare page header footer)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [container-fn content xmap]
  (page/html5
   [:head [:link {:rel "shortcut icon" :href "/images/favicon.png"}]
    (page/include-js "/js/ga.js")
    (page/include-css "/css/priceous.css")
    [:title (:title xmap)]]
   [:body
    [:div {:id "main"}
     (header content)
     [:div {:class "full-container"}
      (container-fn content)]
     (footer content)]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header [content]
  [:div {:class "header"}
   [:div {:class "priceous_logo"}
    [:a {:href "/search" :class "h100"}
     [:img {:src "/images/priceous_logo_2x.png" :width "150" :height "75"}]]]
   
   [:div {:class "mp-root"} 
    [:div {:class "mp-child"}
     [:a {:href "/about"  :class "headerlink"} "О проекте"]]]

   [:div {:class "mp-root"} 
    [:div {:class "mp-child"}
     [:a {:href "/help"  :class "headerlink"} "Помощь"]]]

   [:div {:class "mp-root"} 
    [:div {:class "mp-child"}
     [:a {:href "/stats" :class "headerlink"} "Статистика"]]]

   [:div {:class "mp-root"} 
    [:div {:class "mp-child"}
     [:a {:href "/contacts" :class "headerlink"} "Контакты"]]]

   #_[:div {:class "mp-root"}
    [:div {:class "mp-child"}
     [:a {:href "/scrap" :class "headerlink"} "Собрать сейчас"]]]

   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn footer [content]
  [:div {:class "footer"}
   [:div {:class "footer_left"}
    [:a {:href "/" :class "link"} "priceous.mishadoff.com"]
    [:span {:class "footer_pause"}]
    [:span "Агрегатор цен на алкоголь"]]
   [:div {:class "footer_right"}
    [:div (format "version %s"
               (v/get-version "priceous" "priceous"))]]
   [:div {:class "footer_center"}]
   ])
