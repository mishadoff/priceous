(ns priceous.system.model
  (:require [schema.core :as s]))

(s/defschema Config
  {:app {:name s/Str
         :version s/Str}

   :server {:port s/Int
            :rate-limit s/Int}

   :solr {:host s/Str
          :collection s/Str}

   :postgres {:auto-commit        s/Bool
              :read-only          s/Bool
              :connection-timeout s/Int
              :validation-timeout s/Int
              :idle-timeout       s/Int
              :max-lifetime       s/Int
              :minimum-idle       s/Int
              :maximum-pool-size  s/Int
              :pool-name          s/Str
              :adapter            s/Str
              :username           s/Str
              :password           s/Str
              :database-name      s/Str
              :server-name        s/Str
              :port-number        s/Int
              :register-mbeans    s/Bool}

   :scheduler {:delay s/Int
               :value s/Int
               :time-unit s/Str}

   :alert {:emails [s/Str]
           :from s/Str
           :password s/Str}

   :scrapping {:providers [s/Str]}

   :view {:per-page s/Int}})