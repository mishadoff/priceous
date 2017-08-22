(ns priceous.provider.rozetka-test
  (:require [priceous.provider.rozetka :as rz]
            [priceous.common-test :as ct]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]))

(deftest test--get-categories
  (ct/testing-categories (rz/get-categories rz/provider)))

(deftest test--node->document-edge-cases
  (testing "Empty page"
    (is (nil? (rz/node->document rz/provider nil))))
  )

(deftest test--node->document-happy-path
  (testing "Some page with all properties present"
    (is (= {:alcohol          40.0
            :available        true
            :country          "Шотландия Спейсайд"
            :description      "Фруктовый вкус с нотками ванили, груши, мёда и длительным, сухим послевкусием с нюансами шоколада"
            :image            "https://i2.rozetka.ua/goods/1416961/glenfiddich_5010327000176_images_1416961876.jpg"
            :link             "https://rozetka.com.ua/glenfiddich_5010327000176/p5851263/"
            :name             "Виски Glenfiddich 12 лет выдержки 0.7 л 40% "
            :price            1087.0
            :producer         "Крепкие напитки Glenfiddich"
            :product-code     "Rozetka_5851263"
            :provider         "Rozetka"
            :sale             nil
            :sale-description nil
            :type             "Виски односолодовый"
            :vintage          nil
            :volume           0.7
            :wine_grape       nil}

           (ct/provider-heavy-node-doc
             rz/node->document
             rz/provider
             "https://rozetka.com.ua/glenfiddich_5010327000176/p5851263/")))

    ))