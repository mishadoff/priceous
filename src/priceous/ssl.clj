(ns priceous.ssl)

(defn trust-all-certificates []
  (let [trust (into-array javax.net.ssl.TrustManager
                          [(reify javax.net.ssl.X509TrustManager
                             (getAcceptedIssuers [this])
                             (checkClientTrusted [this certs auth])
                             (checkServerTrusted [this certs auth]))])
        sc (javax.net.ssl.SSLContext/getInstance "SSL")]
    (.init sc nil trust (java.security.SecureRandom.))
    (javax.net.ssl.HttpsURLConnection/setDefaultSSLSocketFactory (.getSocketFactory sc))))
