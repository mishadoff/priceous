(ns priceous.web.templates.contacts
  (:require [hiccup.core :refer :all]
            [priceous.web.templates.base :as base]))

(declare view contacts-container)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn view [response] (base/page contacts-container response {:title "Контакты"}))

(defn- contacts-container [response]
  [:div {:class "contacts-main"}
   [:p "Почта: " [:a {:href "mailto:alcopriceous@gmail.com"
                      :class "link"} "alcopriceous@gmail.com"]]])
