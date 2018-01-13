(ns priceous.provider.elitochka-test
  (:require [priceous.provider.elitochka :as et]
            [priceous.common-test :as ct]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]
            [priceous.utils :as u]))

(deftest test--get-categories
  (ct/testing-categories (et/get-categories et/provider)))

(deftest test--node->document-edge-cases
  (testing "Empty page"
    (is (nil? (et/node->document et/provider nil))))
  )

(deftest test--node->document-happy-path
  (testing "Some page with all properties present"
    (doseq [[test-in test-out test-meta?] (ct/load-cases "test/resources/elitochka")]
      (is (= (u/read-edn test-out)
             (ct/provider-doc et/node->document (ct/apply-meta et/provider test-meta?) test-in))))
    ))

(comment
  (ct/save "https://elitochka.com.ua/catalog/item/2160"
           "test/resources/elitochka/CASE_001_balblair_in.edn")

  )