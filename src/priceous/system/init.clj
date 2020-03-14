(ns priceous.system.init
  (:require [clojure.tools.logging :as log]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]))

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
