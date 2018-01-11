(ns priceous.provider.rozetka-test
  (:require [priceous.provider.rozetka :as rz]
            [priceous.common-test :as ct]
            [priceous.utils :as u]
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
    (doseq [[test-in test-out test-meta?] (ct/load-cases "test/resources/rozetka")]
      (is (= (u/read-edn test-out) (ct/provider-doc rz/node->document
                                                    (ct/apply-meta rz/provider test-meta?)
                                                    test-in))))
      ))

(comment
  (ct/save "https://rozetka.com.ua/glenfiddich_5010327000176/p5851263/"
           "test/resources/rozetka/CASE_001_glenfiddich_in.edn")

  (ct/save "https://rozetka.com.ua/choya_4017871800055/p18863180/"
           "test/resources/rozetka/CASE_002_choya_in.edn")
  )