(ns priceous.utils-test
  (:require [priceous.utils :refer :all]
            [clojure.test :refer :all]))

(deftest smart-parse-double-test
  ;; happy cases
  (is (= 1.0 (smart-parse-double "1")))
  (is (= 1235.7 (smart-parse-double "1235.7")))
  (is (= 12.04 (smart-parse-double "12.04")))

  ;; handle commas gracefully
  (is (= 1.7 (smart-parse-double "1,7")))
  (is (= 123.75 (smart-parse-double "123,75")))

  ;; remove intruders
  (is (= 1.7 (smart-parse-double "1,7л")))
  (is (= 1.7 (smart-parse-double "1.7 литров")))
  (is (= 1.7 (smart-parse-double "   1 , 7  ")))
  (is (= 1.7 (smart-parse-double "  1.7л")))

  ;; empty strings
  (is (nil? (smart-parse-double nil)))
  (is (nil? (smart-parse-double "")))

  ;; other edge cases
  (is (= 0.0 (smart-parse-double "0000")))
  (is (nil? (smart-parse-double "..")))
  
  )



