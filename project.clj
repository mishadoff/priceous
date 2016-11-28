(defproject priceous "0.0.3"
  :description "Process to monitor prices for precious items"
  :url "http://mishadoff.com" ;; TODO Domain?
  :license {:name "Eclipse Public License" ;; TODO License definition
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha3"]

                 ;; Instrumenting and dynamic providers loading 
                 [org.clojure/tools.namespace "0.2.11"]

                 ;; server and middleware
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-json "0.4.0"]

                 ;; routing
                 [compojure "1.4.0"]

                 ;; templating
                 [hiccup "1.0.5"]

                 ;; for crawling
                 [clj-http "2.0.0"]
                 
                 ;; enlive for selector based scrapping
                 [enlive "1.1.6"]
                 
                 ;; logger
                 [com.taoensso/timbre "4.3.1"]
                 [com.fzakaria/slf4j-timbre "0.3.2"]
                 
                 ;; solr client
                 [com.codesignals/flux "0.6.0"
                  :exclusions [org.apache.solr/solr-core]]

                 ;; good times
                 [clj-time "0.11.0"]

                 ;; we also can write to csv
                 [org.clojure/data.csv "0.1.3"]

                 ;; json is a must
                 [cheshire "5.6.3"]

                 ;; Rate limiter
                 [ring-ratelimit "0.2.2"]
                 
                 ]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler priceous.web/app
         :init    priceous.web/init}

  :profiles {:dev {:plugins [[jonase/eastwood "0.2.1"]
                             [lein-kibit "0.1.2"]
                             [lein-bikeshed "0.2.0"]
                             [lein-cloverage "1.0.6"]]}}

  
  :aot  [priceous.web]
  :main priceous.web
  )
