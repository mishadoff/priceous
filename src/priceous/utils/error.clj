(ns priceous.utils.error)

(defn die [message]
  (throw (IllegalArgumentException. message)))
