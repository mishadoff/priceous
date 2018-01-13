(ns priceous.provider.megamarket-test
  (:require [priceous.provider.megamarket :as mm]
            [priceous.common-test :as ct]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]))

(deftest test--get-categories
  (ct/testing-categories (mm/get-categories mm/provider)))

(deftest test--node->document-edge-cases

  (testing "Empty page"
    (is (nil? (mm/node->document mm/provider nil))))
  )

(deftest test--node->document-happy-path
  (testing "Some page with all properties present"
    (doseq [[test-in test-out test-meta?] (ct/load-cases "test/resources/megamarket")]
      (let [provider (ct/apply-meta mm/provider test-meta?)]
        (is (= (u/read-edn test-out)
               (->> (su/find-nodes provider (u/read-edn test-in))
                    (map (fn [node] (ct/provider-doc-by-node mm/node->document provider node)))
                    (first))
               ))))))

(comment
  (ct/save "https://megamarket.ua/catalogue/category/1020"
        "test/resources/megamarket/CASE_001_whisky_in.edn")

  )