(ns priceous.provider)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Accessors and Mutators for provider structure 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;; Getters

(defn current-page [provider]
  (format (get-in provider [:state :page-template])
          (get-in provider [:state :page-current])))

(defn get-page-template
  "Retrieves page-template from provider"
  [provider]
  (get-in provider [:state :page-template]))

(defn get-page-processed
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:state :page-processed]))

(defn get-page-current
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:state :page-current]))

(defn get-page-limit
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:state :page-limit]))

(defn get-page->urls
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:functions :page->urls]))

(defn get-last-page?
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:functions :last-page?]))

(defn get-provider-name
  "Retrieves provider name"
  [provider]
  (get-in provider [:info :name]))

;;; Predicates

(defn limit-reached?
  "Returns true if provider reaches a limit of processed pages"
  [provider]
  (>= (get-page-processed provider)
      (get-page-limit provider)))

(defn done?
  "Is provider in done state?"
  [provider]
  (get-in provider [:state :done]))


;;; Setters

(defn set-done 
  "Set provider state to done"
  [provider]
  (assoc-in provider [:state :done] true))


(defn set-done-if-limit-reached
  [provider]
  (cond 
    (limit-reached? provider) (set-done provider) 
    :else                     provider))
