#!/bin/bash

SERVER_IP="priceous.mishadoff.com"

echo "Build project"
lein uberjar

echo "Deploying SOLR configuration"
scp -r ~/coding/clojure/priceous/resources/solr/whisky root@$SERVER_IP:/root/solr-6.4.1/server/solr/

echo "Deploying jar"
scp ~/coding/clojure/priceous/target/priceous-0.3.0-standalone.jar root@$SERVER_IP:/root/priceous/priceous-0.3.0.jar

echo "Copying configuration"
scp ~/coding/clojure/priceous/resources/priceous.edn root@$SERVER_IP:/root/priceous/

echo "Copying start script"
scp ~/coding/clojure/priceous/scripts/start.sh root@$SERVER_IP:/root/priceous/
