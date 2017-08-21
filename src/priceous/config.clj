(ns priceous.config
  (:require [taoensso.timbre :as log]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [clojure.java.io :as io]))

(def properties (atom nil))

(defn prop
  ([path] (get-in @properties path))
  ([path not-found] (get-in @properties path not-found)))

(defn config-timbre! []
  (let [colors {:info :green :warn :yellow :error :red :fatal :purple :report :blue}]
    (log/set-config!
      {:level :info
       :output-fn log/default-output-fn
       :timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss.SSS"}
       :appenders
       {

        :color-appender
        {:enabled?   true
         :async?     false
         :min-level  nil
         :rate-limit nil
         :output-fn  :inherit
         :fn (fn [{:keys [error? level output-fn] :as data}]
               (binding [*out* (if error? *err* *out*)]
                 (if-let [color (colors level)]
                   (println (log/color-str color (output-fn data)))
                   (println (output-fn data)))))}

        :rotor-appender
        (rotor/rotor-appender {:path "./priceous.log"})

        }})))

(defn- props-from-resource [file]
  (try (read-string (slurp (io/resource file)))
       (catch Exception e
         (do (log/error e "Problem reading props from resource") {} ))))

(defn- props-from-file [file]
  (log/debug "Reading props from file" file)
  (try (read-string (slurp (io/file file)))
       (catch Exception e
         (do (log/error e "Problem reading props from file") {} ))))

;; to avoid inonsistent properties we allow to read them only once
(defn read-properties! [external-file]
  (cond @properties @properties
        :else (do
                (log/debug "Fill properties -> atom...")
                (let [external-map (if external-file (props-from-file external-file) {})
                      internal-map (props-from-resource "priceous.edn") ]
                  (reset! properties (merge internal-map external-map))))))
