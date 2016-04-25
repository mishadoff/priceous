(defproject priceous "0.0.1-SNAPSHOT"
  :description "Process to monitor prices for precious items"
  :url "http://mishadoff.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.4.0"]

                 
                 ;; templating
                 [hiccup "1.0.5"]
                 
                 [clj-http "2.0.0"]
                 
                 ;; enlive (for scrapping)
                 [enlive "1.1.6"]
                 
                 ;; logger
                 [com.taoensso/timbre "4.3.1"]
                 
                 ;; solr client
                 [com.codesignals/flux "0.6.0" :exclusions [org.apache.solr/solr-core]]

                 ;; good times
                 [clj-time "0.11.0"]

                 ;; we write to csv
                 [org.clojure/data.csv "0.1.3"]

                 
                 ]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler priceous.web/app}
  
  ;;:aot  [priceous.core]
  ;;:main priceous.core
  )
