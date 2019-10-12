(ns priceous.spider.writer
  (:require [taoensso.timbre :as log]
            [priceous.solr :as solr]))

(defmulti append (fn [apn _ _] apn))

;;;

(defmethod append :solr [apn provider items]
  (log/info "Start writing data into SOLR...")
  (solr/write provider items)
  (log/info "Writing data into SOLR DONE."))

(defmethod append :default [apn provider items]
  (log/error (format "Cannot found appender for %s" apn)))
