(defproject priceous "0.3.0"
  :description "Price aggregator for alcohol products"
  :url "http://priceous.mishadoff.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]

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

                 ;; for api based scrapping
                 [clj-http "2.0.0"]

                 ;; for selector based scrapping
                 [enlive "1.1.6"]

                 ;; logger
                 [com.taoensso/timbre "4.7.4"]
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

                 ;; Rate limiter per IP / revisit
                 [ring-ratelimit "0.2.2"]

                 ;; under dmi3iy influence
                 [org.clojure/test.check "0.9.0"]

                 ;; extract version of the project into app
                 [trptcolin/versioneer "0.2.0"]

                 ;; Mail library for alerts
                 [com.draines/postal "2.0.2"]]

                 ;; Selenium


  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler priceous.web/app
         :init    priceous.web/init}

  :profiles {:dev {:plugins [[jonase/eastwood "0.2.1"]
                             [lein-kibit "0.1.2"]
                             [lein-bikeshed "0.2.0"]
                             [lein-cloverage "1.0.6"]]}}

  ;; no opts for now
  :jvm-opts []

  :aot  [priceous.web]
  :main priceous.web)

