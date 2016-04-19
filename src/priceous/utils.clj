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
  [{:keys [provider] :as context} required? fun selectors-to-log]
  (try
    (fun)
    (catch NoSuchElementException e
      (let [message (format
                     "No elements found on provider [%s] using selectors %s"
                     provider selectors-to-log)]
        (cond
          required? (log/error message)
          :else     (log/warn message)
          )
        ))))

(defn debug [e]
  (log/debug e)
  e)
