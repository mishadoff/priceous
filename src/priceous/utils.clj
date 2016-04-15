(ns priceous.utils
  (:require [clj-webdriver.taxi :as web]
            [taoensso.timbre :as log])
  (:import [org.openqa.selenium NoSuchElementException]))

;;;;;;;;;;;;;;;;;;;;;;
;; Common Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn selenium-failsafe-apply
  "Apply selenium function with provided selector
  if element can't be found log error to track the issue in your selector
  and return nil instead of throwing exception"
  [{:keys [provider] :as context} fun selectors-to-log]
  (try
    (fun)
    (catch NoSuchElementException e
      (log/error
       (format
        "No elements found on provider [%s] using selectors %s"
        provider selectors-to-log)))))
