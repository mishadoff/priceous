(ns priceous.utils
  (:refer-clojure :exclude [find])
  (:require [clj-webdriver.taxi :as web]))
  
;;;;;;;;;;;;;;;;;;;;;;
;; Common Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn find [item class]
  (web/find-element-under item {:class class}))
