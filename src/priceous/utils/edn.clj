(ns priceous.utils.edn
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

;;;

(defn read-edn [path]
  (try (read-string (slurp (io/file path)))
       (catch Exception e
         (do (log/error e (format "Problem reading props from file [%s]" path)) {}))))

;;;

(defn read-edn-silent [path]
  (try (read-string (slurp (io/file path))) (catch Exception e {})))

;;;