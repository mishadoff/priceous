(ns priceous.experimental.selenium
  (:require [sparkledriver.browser :refer [with-browser make-browser fetch!]])
  (:require [sparkledriver.element :refer [find-by-xpath* text]]))


(defn run []
  (with-browser [browser (make-browser)]
                (-> (fetch! browser "http://clojure.org")))
  )