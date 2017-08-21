(ns priceous.provider.goodwine-test
  (:require [priceous.provider.goodwine :as gw]
            [priceous.common-test :as ct]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]
            [priceous.utils :as u]))

(deftest test--get-categories
  (ct/testing-categories (gw/get-categories gw/provider)))

(deftest test--node->document-edge-cases

  (testing "Empty page"
    (is (nil? (gw/node->document gw/provider nil))))

  (testing "No processing when .mainInfoBlock is unavailable"
    (is (nil? (gw/node->document
                gw/provider
                {:page (enlive/html-snippet "<div class='_mainInfoProd'>something</div>")
                 :node nil :link nil}))))
  )

(deftest test--node->document-happy-path
  (testing "Some real world pages"
    (is (= {:alcohol          40.0
            :available        true
            :country          "Великобритания - Шотландия"
            :description      "Золотого цвета с отчетливо свежим, фруктовым ароматом с нотками груш и элегантным балансом. Во вкусе богатый, сладкий с фруктовыми нотами, постепенно развивается в оттенки ирисок, сливок, солода с мягкими тонами дуба. Финиш продолжительный, гладкий и мягкий."
            :excise           true
            :image            "http://goodwine.com.ua/media/catalog/product/cache/1/image/119x450/9df78eab33525d08d6e5fb8d27136e95/0/1/01347.jpg"
            :item_new         nil
            :link             "http://goodwine.com.ua/glenfiddich-12-yo-tube-01347.html"
            :name             "Виски Glenfiddich 12 yo, tube 0,7 л"
            :price            1190.0
            :producer         "Glenfiddich"
            :product-code     "Goodwine_01347"
            :provider         "Goodwine"
            :sale             false
            :sale-description nil
            :trusted          true
            :type             "Крепкие Односолодовый"
            :vintage          nil
            :volume           0.7
            :wine_grape       nil
            :wine_sugar       nil}
           (ct/provider-heavy-node-doc
             gw/node->document
             (assoc-in gw/provider [:state :category] "Крепкие")
             "http://goodwine.com.ua/glenfiddich-12-yo-tube-01347.html")))

    (is (= {:alcohol          13.0
            :available        true
            :country          "Новая Зеландия - Мальборо"
            :description      "Оптимальная спелость Совиньон Блана проявляется в выразительном, пышном аромате маракуйи, крыжовника, листьев черной смородины в обрамлении тонких цитрусовых тонов розового грейпфрута и жимолости. Во вкусе вино пронзительно свежее, обладает сбалансированной кислотностью и изящной минеральностью. Послевкусие приятное, освежающее. Отлично сочетается со свежими морепродуктами, овощами гриль и белым мясом."
            :excise           true
            :image            "http://goodwine.com.ua/media/catalog/product/cache/1/image/119x450/9df78eab33525d08d6e5fb8d27136e95/0/5/05776.jpg"
            :item_new         nil
            :link             "http://goodwine.com.ua/sauvignon-blanc-marlborough-sun-05776.html"
            :name             "Вино Sauvignon Blanc Marlborough Sun 0,75 л"
            :price            270.0
            :producer         "Saint Clair"
            :product-code     "Goodwine_05776"
            :provider         "Goodwine"
            :sale             true
            :sale-description "Цена при покупке любых 6+ бутылок, 249.00 грн"
            :trusted          true
            :type             "Вино Белое Сухое"
            :vintage          nil
            :volume           0.75
            :wine_grape       "Совиньйон Блан"
            :wine_sugar       3.5}
           (ct/provider-heavy-node-doc
             gw/node->document
             (assoc-in gw/provider [:state :category] "Вино")
             "http://goodwine.com.ua/sauvignon-blanc-marlborough-sun-05776.html")))

    ))