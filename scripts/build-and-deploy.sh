#!/bin/bash

echo "Build project"
lein uberjar

echo "Deploying SOLR configuration"
scp -r ~/coding/clojure/priceous/resources/solr/whisky root@priceous.mishadoff.com:/root/solr-6.4.1/server/solr/

echo "Deploying jar"
scp ~/coding/clojure/priceous/target/priceous-0.2.0-standalone.jar root@priceous.mishadoff.com:/root/priceous/priceous-0.2.0.jar

echo "Copying configuration"
scp ~/coding/clojure/priceous/resources/priceous.edn root@priceous.mishadoff.com:/root/priceous/
