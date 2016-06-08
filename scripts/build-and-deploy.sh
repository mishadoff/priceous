#!/bin/bash

echo "Build project"
lein uberjar

echo "Deploying SOLR configuration"
scp -r ~/soft/solr-6.0.0/server/solr/whisky/ root@146.185.149.119:/root/solr/solr-6.0.0/server/solr/

echo "Deploying jar"
scp ~/coding/clojure/priceous/target/priceous-0.0.1-SNAPSHOT-standalone.jar root@146.185.149.119:/root/priceous/priceous-0.0.1.jar
