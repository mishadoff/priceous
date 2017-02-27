(ns priceous.templates.stats
  (:require [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [priceous.templates.base :as base]
            [priceous.utils :as u]
            [taoensso.timbre :as log]))

(declare view stats-container)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn view [content] (base/page stats-container content {:title "Статистика"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn stats-container [content]
  (let [{:keys [status response]} content]
    (cond
      (not= :success status) [:div "Статистика недоступна."]
      
      :else
      [:div {:class "stats-main"}
       [:table {:class "stats-table"}
        [:tr
         [:th "Магазин"]
         [:th "#Товаров"]
         [:th "#В наличии"]
         [:th "Последняя сборка"]]
        
        (for [p (get-in response [:providers])]
          [:tr
           [:td
            [:span {:class "item-provider"}
             (let [p (u/resolve-provider-by-name (:name p))]
               [:a {:href (get-in p [:info :base-url])}
                [:img {:class "nested_fixed_img"
                       :src (get-in p [:info :icon])
                       :title (:name p)
                            :alt (:name p)}]])]
            
            ]
           [:td (:total p)]
           [:td (:available p)]
           [:td
            (let [readable-time (u/readable-time (:ts p))
                  fmt-time (-> (:ts p)
                               (clojure.string/replace "T" " ")
                               (clojure.string/replace "Z" " "))]
              (condp = readable-time
                "Сегодня" [:span {:title fmt-time :class "stats-time-today"} readable-time]
                "Вчера" [:span {:title fmt-time :class "stats-time-yesterday"} readable-time]
                "Давно" [:span {:title fmt-time :class "stats-time-outdated"} readable-time]
                readable-time))]])

        [:tr {:class "stats-last-row"}
         [:td "Всего"] [:td (get-in response [:total])]]

        ]])))
