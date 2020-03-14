(ns priceous.utils.debug
  (:require [clojure.tools.logging :as log]))

(defn debug [e] (log/debug e) e)

(defn debug-lens [e f]
  (log/debug "\tDEBUGGING LENS\t" (f e))
  e)

