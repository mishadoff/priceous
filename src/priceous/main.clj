(ns priceous.main
  (:gen-class)
  (:require [priceous.system :as system]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;(defn init
;  ([] (init ["external_config.edn"]))
;  ([args]
;   (ssl/trust-all-certificates)
;   (config/config-timbre!)
;   (config/read-properties! (first args))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn -main [& args]
  ;(init args)
  ;(log/info "Server conf: " @config/properties)
  ;(log/info "Server started.")
  ;(init-scheduler)
  (system/start)
  )
