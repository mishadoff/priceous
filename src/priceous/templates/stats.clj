(ns priceous.templates.stats
  (:require [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [priceous.templates.base :as base]
            [taoensso.timbre :as log]))

(declare view stats-container)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn view [content] (base/page stats-container content))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn stats-container [content]
  [:div {:class "full-container"}
   (let [{:keys [status response]} content]
     (cond

       (not= :success status) [:div "Статистика недоступна."]

       :else
       [:div
        [:ul {:class "help-list"}

         ;; Totals
         [:li 
          [:div (format "В базе всего %s элементов" (get-in response [:total]))]]
         
         ;; Providers
         [:li
          [:div (format "Доступна информация по %s магазинам (в наличии / всего / последняя сборка)"
                        (count (get-in response [:providers])))]
          [:ul
           (for [p (get-in response [:providers])]
             [:li (format "%s: %s/%s, %s"
                          (:name p)
                          (:available p)
                          (:total p)
                          (-> (:ts p)
                              (clojure.string/replace "T" " ")
                              (clojure.string/replace "Z" " ")))])]]]]))])
