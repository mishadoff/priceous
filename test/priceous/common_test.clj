(ns priceous.common-test
  (:require [clojure.test :refer :all]))

;; common package for testing different providers

(defn testing-categories [cats]
  (testing "Just validate get-categories follows the contract"
    (is (pos? (count cats)))                                ;; at least one category exists
    (is (vector? cats))                                     ;; we don't break the type

    (doseq [{name :name template :template} cats]           ;; for each category
      (is (not (empty? name)))                              ;; category name is not empty
      (is (.startsWith template "http://"))                 ;; template is url
      (is (.contains template "%s"))                        ;; template has a placeholder
      )

    ;; no duplicate category names found
    (is (= (count cats) (count (->> cats (map :name) (into #{})))))
    ;; no duplicate urls found
    (is (= (count cats) (count (->> cats (map :template) (into #{})))))

    ))
