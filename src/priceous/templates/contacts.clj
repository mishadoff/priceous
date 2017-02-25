(ns priceous.templates.contacts
  (:require [hiccup.core :refer :all]
            [priceous.templates.base :as base]))

(declare view contacts-container)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn view [response] (base/page contacts-container response))

(defn- contacts-container [response]
  [:div {:class "contacts-main"}
   [:p "Почта: " [:a {:href "mailto:alcopriceous@gmail.com"
                      :class "link"} "alcopriceous@gmail.com"]]])
