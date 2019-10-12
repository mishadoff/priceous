(ns priceous.utils.debug
  (:require [taoensso.timbre :as log]))

(defn debug [e] (log/debug e) e)

(defn debug-lens [e f]
  (log/debug "\tDEBUGGING LENS\t" (f e))
  e)

