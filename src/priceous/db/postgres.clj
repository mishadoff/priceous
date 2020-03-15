(ns priceous.db.postgres
  (:require [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [clj-time.coerce :as tc])
  (:import (clojure.lang Keyword)
           (java.util Date)
           (org.joda.time DateTime)
           (java.sql Timestamp)))

(hugsql/def-db-fns "db/queries.sql")

(defn keyword->string []
  (extend-protocol jdbc/ISQLValue
    Keyword
    (sql-value [value]
      (str value))))

;;;

(defn java-date->timestamp []
  (extend-protocol jdbc/ISQLValue
    Date
    (sql-value [value]
      (tc/to-sql-time value))))

;;;

(defn joda-time->timestamp []
  (extend-protocol jdbc/ISQLValue
    DateTime
    (sql-value [value]
      (tc/to-sql-time value))))

;;;

(defn sql-timestamp->joda-time []
  (extend-protocol jdbc/IResultSetReadColumn
    Timestamp
    (result-set-read-column [value _ _]
      (tc/from-long (.getTime value)))))

;;;

(defn register-postgres-type-bindings []
  ;; Clojure -> SQL conversions
  (keyword->string)
  (java-date->timestamp)
  (joda-time->timestamp)
  ;; SQL -> Clojure conversion
  (sql-timestamp->joda-time))