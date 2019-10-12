(ns priceous.system.config
  (:refer-clojure :exclude [get])
  (:require [taoensso.timbre :as log]
            [priceous.utils.collections :as collections]
            [priceous.system.model :as model]
            [priceous.system.state :as state]
            [clojure.java.io :as io]
            [schema.core :as s]))

;;;

(defn get [& ks]
  (->> (into [:system/config] ks)
       (get-in state/system)))

;;;

(defn- from-resource [file]
  (try (read-string (slurp (io/resource file)))
       (catch Exception e
         (do (log/error e "Problem reading props from resource") {}))))

(defn- from-external-file [file]
  (log/debug "Reading props from file" file)
  (try (read-string (slurp (io/file file)))
       (catch Exception e
         (do (log/error e "Problem reading props from file") {}))))

;;;

(defn read-config!
  "Read properties from several locations and merging theem into one map. Last wins."
  []
  (->> (collections/deep-merge
         (from-resource "default.edn")
         (from-external-file "config.edn"))
       (s/validate model/Config)))

;;;