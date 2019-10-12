(ns priceous.system.model
  (:require [schema.core :as s]))

(s/defschema Config
  {:app {:name s/Str
         :version s/Str}

   :server {:port s/Int
            :rate-limit s/Int}

   :solr {:host s/Str
          :collection s/Str}

   :scheduler {:delay s/Int
               :value s/Int
               :time-unit s/Str}

   :alert {:emails [s/Str]
           :from s/Str
           :password s/Str}

   :scrapping {:providers [s/Str]}

   :view {:per-page s/Int}})