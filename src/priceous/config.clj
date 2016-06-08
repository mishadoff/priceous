(ns priceous.config
  (:require [taoensso.timbre :as timbre]))

(defn config-timbre! []
  (let [colors {:info :green :warn :yellow :error :red :fatal :purple :report :blue}]
    (timbre/set-config!
      {:level :debug
       :output-fn timbre/default-output-fn
       :appenders
       {:color-appender
         {:enabled?   true
          :async?     false
          :min-level  nil
          :rate-limit nil
          :output-fn  :inherit
          :fn (fn [{:keys [error? level output-fn] :as data}]
                (binding [*out* (if error? *err* *out*)]
                  (if-let [color (colors level)]
                    (println (timbre/color-str color (output-fn data)))
                    (println (output-fn data)))))}}})))
