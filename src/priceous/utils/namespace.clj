(ns priceous.utils.namespace
  (:require [clojure.tools.namespace :as cns]
            [clojure.tools.logging :as log]))

(defn find-all-providers []
  (->> (cns/find-namespaces-on-classpath)
       (map str)
       (filter (fn [ns-name] (clojure.string/starts-with? ns-name "priceous.provider.")))
       (filter (fn [ns-name]
                 (not (nil?
                        (some-> (format "%s/provider" ns-name)
                                symbol
                                resolve
                                var-get)))))
       (map (fn [ns-name] (last (seq (.split ns-name "\\.")))))
       (doall)))

;;;

(defn require-all-providers []
  (require 'priceous.provider.goodwine)
  ;; Require all namespaces in priceous.provider.* folder
  #_(let [symbols (->> (cns/find-namespaces-on-classpath)
                       (filter (fn [sym] (clojure.string/starts-with? (str sym) "priceous.provider."))))]
      (doseq [provider-ns symbols]
        (require provider-ns))))

;;;

(defn resolve-provider-by-name [pname]
  (try (-> (format "priceous.provider.%s/provider" pname)
           symbol
           resolve
           var-get)
       (catch Exception e
         (log/error (format "Can not resolve provider [%s] %s" pname (.getMessage e))))))

;;;
