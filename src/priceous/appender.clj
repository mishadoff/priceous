(ns priceous.appender
  (:require [taoensso.timbre :as log]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [priceous.config :as config]
            [priceous.solr :as solr]))

(defmulti append (fn [apn _ _] apn))

(defmethod append :csv [apn provider items]
  (log/info "Writing data into CSV")
  (let [header (->> items (mapcat keys) (distinct) (into []))
        strdata (->> items
                     (map (fn [m] (vec (for [h header] (m h)))))
                     (into [header]))]
    (with-open [out (io/writer (config/prop [:csv :filepath]))]
      (csv/write-csv out strdata)))
  (log/info "Writing data into CSV DONE."))

(defmethod append :solr [apn provider items]
  (log/info "Start writing data into SOLR...")
  (solr/write provider items)
  (log/info "Writing data into SOLR DONE."))

(defmethod append :default [apn provider items]
  (log/error (format "Cannot found appender for %s" apn)))
