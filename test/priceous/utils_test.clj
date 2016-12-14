(ns priceous.utils-test
  (:require [priceous.utils :refer :all]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            ))

;; make sure debug does not corrupt messages
(defspec debug--equal-property-test 
  (prop/for-all [e gen/simple-type] (= e (debug e))))


;; Does not work :(
;;
;; (defspec die--exception-created-and-contains-message
;;   (prop/for-all [message gen/string]
;;                 (try (falsy (die message))
;;                      (catch IllegalArgumentException ex
;;                        (.contains (.getMessage ex) message)))))

(defspec now--looks-like-a-valid-string 
  (prop/for-all [e (gen/elements [(now)])]
                (and (string? e) (= 20 (count e))
                     (.contains e "T") (.contains e "Z"))))

(deftest to-date-test
  (testing "Conversion works"
    (is (= "2016-06-08T12:26:17Z" (to-date 1465388777222)))))

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
  (is (false? ((falsy) "some arg")))
  ;; var args
  (is (false? ((falsy) 1 2 3 nil nil :some "ARGS"))))

(deftest truthy-test
  ;; no-args
  (is (true? ((truthy))))
  ;; one arg
  (is (true? ((truthy) "some arg")))
  ;; var args
  (is (true? ((truthy) 1 2 3 nil nil :some "ARGS"))))

(deftest get-client-ip-test
  (is (= "10.2.1.1" (get-client-ip {:headers {"x-forwarded-for" "10.2.1.1"}})))
  (is (= "10.2.1.1" (get-client-ip {:headers {"x-forwarded-for" "10.2.1.1,10.2.1.2"}})))
  )

(defspec elapsed-so-far--time-is-greater-than-passed
  5 ;; run only 5 tests so far
  (prop/for-all [pause (gen/choose 10 1000)]
                (do
                  (Thread/sleep pause)
                  (> (elapsed-so-far pause) (/ pause 1000.0)))))

(deftest full-href-test
  (is (= "a/b" (full-href {:info {:base-url "a"}} "b")))
  (is (= "a/b" (full-href {:info {:base-url "a/"}} "b")))
  (is (= "a/b" (full-href {:info {:base-url "a"}} "/b")))
  (is (= "a/b" (full-href {:info {:base-url "a/"}} "/b"))))
