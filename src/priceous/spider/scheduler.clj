(ns priceous.scheduler
  (:import [java.util.concurrent Executors TimeUnit]))

;; ONLY ONE SCHEDULED PROCESS AT A TIME SUPPORTED
(def ^:private scheduler (Executors/newSingleThreadScheduledExecutor))
(def ^:private scheduler-future-atom (atom nil))

(declare schedule-submit-function
         schedule-cancel)

(defn schedule-submit-function
  "Submit function which will be called periodically
  with the provided period. By default period set to 1 SECOND"
  [fun & {:keys [delay value time-unit]
          :or {delay 0 value 1 time-unit TimeUnit/SECONDS}}]
  ;; cancel previous function
  (schedule-cancel)
  ;; submit new function
  (reset! scheduler-future-atom
          (.scheduleAtFixedRate 
           scheduler fun delay value time-unit)))

(defn schedule-cancel
  "Cancel running function"
  []
  (when @scheduler-future-atom
    (.cancel @scheduler-future-atom true)))
