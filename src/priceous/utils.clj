(ns priceous.utils
  (:refer-clojure :exclude [find])
  (:require [clj-webdriver.taxi :as web])
  (:import [org.openqa.selenium NoSuchElementException]))
  
;;;;;;;;;;;;;;;;;;;;;;
;; Common Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn find [item class]
  (try
    (web/find-element-under item {:class class})
    (catch NoSuchElementException e nil)))
