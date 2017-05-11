(ns priceous.provider.goodwine-test
  (:require [priceous.provider.goodwine :as gw]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]))

(deftest test--get-categories
  (testing "Just validate get-categories follows the contract"
    (let [cats (gw/get-categories gw/provider)]
      ;; TODO extract to common_test
      (is (pos? (count cats))) ;; at least one category exists
      (is (vector? cats))      ;; we don't break the type

      (doseq [{name :name template :template} cats] ;; for each category
        (is (not (empty? name))) ;; category name is not empty
        (is (.startsWith template "http://")) ;; template is url
        (is (.contains template "%s")) ;; template has a placeholder
        )

      ;; no duplicate categeory names found
      (is (= (count cats) (count (->> cats (map :name) (into #{})))))
      ;; no duplicate urls found
      (is (= (count cats) (count (->> cats (map :template) (into #{})))))

      )))

(deftest test--node->document-edge-cases

  (testing "Empty page"
    (is (nil? (gw/node->document gw/provider nil))))

  (testing "No processing when .mainInfoBlock is unavailable"
    (is (nil? (gw/node->document
                gw/provider
                {:page (enlive/html-snippet "<div class='_mainInfoProd'>something</div>")
                 :node nil :link nil}))))
  )

;; TODO introduce test resources
(deftest test--node->document-happy-path
  (testing "Some page with all properties present"
    (let [page-hiccup
          [:div {:class "mainInfoProd"}

           ;; title * product code
           [:div {:class "titleProd"}
            [:h1 "Виски Springbank 10yo (0,7л)"
             [:p {:class "articleProd"}
              "Арт." [:span "12345"]]]]

           ;; image
           [:div {:class "imageDetailProd clearfix"}
            [:div {:class "medalIcon"}
             [:img {:class "stamp" :src "/New_Item"}]]
            [:div {:class "medalIcon"}
             [:img {:class "stamp" :src "/Sale_Item"}]]
            [:div {:class "selected"}
             [:div {:mag-thumb "inner"}
              [:img {:id  "mag-thumb"
                     :src "imglink"}]]]]

           ;; specs
           [:div {:class "innerDiv"}
            [:h2 {:class "innerTitleProd"} "география"]
            "Шотландия"]
           [:div {:class "innerDiv"}
            [:h2 {:class "innerTitleProd"} "Сахар, г/л"]
            "12,7 г/л"]
           [:div {:class "innerDiv"}
            [:h2 {:class "innerTitleProd"} "Сортовой состав"]
            "Пино нуар"]
           [:div {:class "innerDiv"}
            [:h2 {:class "innerTitleProd"} "Винтаж"]
            "1984"]
           [:div {:class "innerDiv"}
            [:h2 {:class "innerTitleProd"} "Производитель"]
            "Springbank Inc."]
           [:div {:class "innerDiv"}
            [:h2 {:class "innerTitleProd"} "Тип"]
            "Виски"]
           [:div {:class "innerDiv"}
            [:h2 {:class "innerTitleProd"} "Крепость, %"]
            "43,7%"]
           [:div {:class "innerDiv"}
            [:h2 {:class "innerTitleProd"} "цвет, вкус, аромат"]
            "Слегка торфяной с нотками цитрусовых"]

           [:div {:class "additionallyServe"}
            [:form
             [:div {:class "bottle"} [:p "Бутылка 0,7 л"]]
             [:div {:class "price"} " 1234 " [:sup "грн"]]
             ]]

           ]]
      (is (= {:provider         "Goodwine"
              :name             "Виски Springbank 10yo (0,7л)"
              :link             "http://somelink"
              :image            "imglink"
              :country          "Шотландия"
              :wine_sugar       12.7
              :wine_grape       "Пино нуар"
              :vintage          "1984"
              :producer         "Springbank Inc."
              :type             "Крепкие Виски"
              :alcohol          43.7
              :description      "Слегка торфяной с нотками цитрусовых"
              :product-code     "Goodwine_12345"
              :available        true
              :item_new         true
              :volume           0.7
              :price            1234.0
              :sale             false
              :sale-description nil
              }

             (-> (gw/node->document
                   (assoc-in gw/provider [:state :category] "Крепкие")
                   {:page (-> page-hiccup hiccup/html enlive/html-snippet)
                    :link "http://somelink"})
                 (dissoc :timestamp)))))))