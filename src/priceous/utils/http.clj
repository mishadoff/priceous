(ns priceous.utils.http
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]))

;;;

(defn full-href [provider part-href]
  (let [base-url (get-in provider [:info :base-url])]
    (str
      (if (s/ends-with? base-url "/")
        (subs base-url 0 (dec (count base-url)))
        base-url)
      "/"
      (if (s/starts-with? part-href "/")
        (subs part-href 1)
        part-href))))

;;;

(defn get-client-ip [req]
  (if-let [ips (get-in req [:headers "x-forwarded-for"])]
    (-> ips (clojure.string/split #",") first)
    (:remote-addr req)))

;;;

(defn fetch [url]
  (log/trace "Fetching URL: " url)
  (try
    (html/html-resource (java.net.URL. url))
    (catch Exception e (log/error e) nil)))