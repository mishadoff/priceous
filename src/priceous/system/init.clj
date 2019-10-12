(ns priceous.system.init
  (:require [taoensso.timbre :as log]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]))

;;;

(defn config-timbre! []
  (let [colors {:info :green :warn :yellow :error :red :fatal :purple :report :blue}]
    (log/set-config!
      {:level          :info
       :output-fn      log/default-output-fn
       :timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss.SSS"}
       :appenders      {:color-appender
                        {:enabled?   true
                         :async?     false
                         :min-level  nil
                         :rate-limit nil
                         :output-fn  :inherit
                         :fn         (fn [{:keys [error? level output-fn] :as data}]
                                       (binding [*out* (if error? *err* *out*)]
                                         (if-let [color (colors level)]
                                           (println (log/color-str color (output-fn data)))
                                           (println (output-fn data)))))}

                        :rotor-appender
                        (rotor/rotor-appender {:path "./priceous.log"})}})))

;;;

(defn trust-all-certificates!
  "This needed to disable certificate validity check between client and server"
  []
  (let [trust (into-array javax.net.ssl.TrustManager
                          [(reify javax.net.ssl.X509TrustManager
                             (getAcceptedIssuers [this])
                             (checkClientTrusted [this certs auth])
                             (checkServerTrusted [this certs auth]))])
        sc (javax.net.ssl.SSLContext/getInstance "SSL")]
    (.init sc nil trust (java.security.SecureRandom.))
    (javax.net.ssl.HttpsURLConnection/setDefaultSSLSocketFactory (.getSocketFactory sc))))

;;;

;(defn init-scheduler []
;  (log/info "SYSTEM_INIT [scheduler]")
;  (scheduler/schedule-submit-function
;   (fn []
;     (Thread/sleep 3000)
;     (log/info "Start scrapping..")
;     (core/scrap (config/prop [:scrapping :providers])))
;   :delay (config/prop [:scheduler :delay])
;   :value (config/prop [:scheduler :value])
;   :time-unit (TimeUnit/valueOf (config/prop [:scheduler :time-unit]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
