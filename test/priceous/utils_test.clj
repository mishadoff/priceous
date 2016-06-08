(ns priceous.utils-test
  (:require [priceous.utils :refer :all]
            [clojure.test :refer :all]))

(deftest die-test
  (testing "Errors thrown" 
    (is (thrown? IllegalArgumentException (die "Clojure Error")))
    (is (thrown? AssertionError (die nil))))

  (testing "Messages are available in exception"
    (let [message "Error message which should be available in ex"]
      (try
        (die message)
        (is false) ;; should not enter to this
        (catch IllegalArgumentException e
          (is (= message (.getMessage e))))))))

(deftest debug-test
  (testing "Do not corrupt input"
    (let [e "Test object for debug"]
      (is (= e (debug e))))))

(deftest now-test
  (testing "Just check what we can check"
    (is (string? (now)))))

(deftest to-date-test
  (testing "Conversion works"
    (is (= "2016-06-08T12:26:17Z"
           (to-date 1465388777222)))))

(deftest fetch-test
  (testing "Hopefully google.com is not down"
    (is (= 2 (count (fetch "http://google.com")))))
  (testing "Malformed URL"
    (is (nil? (fetch "blablabla"))))
  (testing "UnknownHost URL"
    ;; if some-one register such domain, comment the test.
    (is (nil? (fetch "http://clojure-is-the-best-language.bla.bla.com"))))
  (testing "Resource Not Found"
    ;; if some-one register such domain, comment the test.
    (is (nil? (fetch "http://google.com/404")))))

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

(deftest cleanup-test
  (is (= "CLEAN" (cleanup "CLEAN")))
  (is (= "CLEAN" (cleanup "       CLEAN")))
  (is (= "CLEAN" (cleanup "CLEAN       ")))
  (is (= "CLEAN" (cleanup "       CLEAN       ")))
  (is (= "CLE AN" (cleanup " CLE AN "))))

(deftest falsy-test
  ;; no-args
  (is (false? ((falsy))))
  ;; one arg
  (is (false? ((falsy) "soem arg")))
  ;; var args
  (is (false? ((falsy) 1 2 3 nil nil :some "ARGS"))))



