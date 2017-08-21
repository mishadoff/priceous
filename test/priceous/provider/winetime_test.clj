(ns priceous.provider.winetime-test
  (:require [priceous.provider.winetime :as wt]
            [priceous.common-test :as ct]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]))

(deftest test--get-categories
  (ct/testing-categories (wt/get-categories wt/provider)))

(deftest test--node->document-edge-cases

  (testing "Empty page"
    (is (nil? (wt/node->document wt/provider nil))))
  )

(deftest test--node->document-happy-path
  (testing "Some page with all properties present"
    (let [page-hiccup
          [:div {:class "product-details-wraper"}
           [:h1 "Виски Springbank 10yo (0,7л)"]

           [:div {:class "foto_main"}
            [:a {:class "badge-new"}]
            [:a [:img {:src "/springbank.jpg"}]]]

           [:table {:class "details_about"}
            [:tr
             [:td
              [:p "Тип: " [:strong [:a "виски"]]]
              [:p "Классификация: " [:strong [:a "бурбон"]]]
              [:p "Страна: " [:strong [:a "США"]]]]
             [:td
              [:p "Регион: " [:strong [:a "Кентукки"]]]
              [:p "Производитель: " [:strong [:a "Maker's Mark Distillery"]]]
              [:p "Объём: " [:strong [:a "0,7 л"]]]]
             ]]

           [:div {:class "harakter_tovar"}
            [:h2 "Tech"]
            [:p [:strong "Алкоголь"] ": 45,00%"]
            [:p [:strong "Классификация"] ": бурбон"]
            [:p [:strong "Объём"] ": 0,7 л"]


            [:p [:strong "Дегустации"] ": Ваниль"]
            [:p [:strong "Аромат"] ": Солод"]
            [:p [:strong "Вкус"] ": Торф и лимонная цедра"]

            ]

           [:div {:class "product-details_info-block"}
            [:h2 {:class "pull-left"}
             [:span "12345"]]]

           [:div {:class "buying_block_do"}
            [:table
             [:tr
              [:td
               [:span {:class "show_all_sum"}
                "1647" [:sup "00"]] "грн"]]]]

           [:div {:class "buying_block_compare"}
            "Старая цена:"
            [:span "1999.00грн"]
            "Экономия: "
            [:span "357.00грн"]]

           ]

          context-node nil
          ]
      (is (= {:provider         "Winetime"
              :name             "Виски Springbank 10yo (0,7л)"
              :link             "http://somelink"
              :image            "http://winetime.com.ua/springbank.jpg"
              :country          "США Кентукки"
              :wine_sugar       nil
              :wine_grape       nil
              :vintage          nil
              :producer         "Maker's Mark Distillery"
              :type             "виски бурбон"
              :alcohol          45.0
              :description      "Ваниль; Солод; Торф и лимонная цедра"
              :product-code     "Winetime_12345"
              :available        true
              :item_new         true
              :volume           0.7
              :price            1647.0
              :sale             true
              :sale-description "старая цена 1999.00"
              :excise           true
              :trusted          true
              }

             (-> (wt/node->document
                   (assoc-in wt/provider [:state :category] "Крепкие")
                   {:page (-> page-hiccup hiccup/html enlive/html-snippet)
                    :node (-> context-node hiccup/html enlive/html-snippet)
                    :link "http://somelink"})
                 (dissoc :timestamp)))))))