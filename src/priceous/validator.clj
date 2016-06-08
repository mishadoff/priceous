(ns priceous.validator
  (:require [clojure.spec :as spec]))

(defn pre-provider [provider]
  {:pre [(spec/valid? :priceous.spec/provider provider)]})
