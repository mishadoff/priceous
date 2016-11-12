#!/bin/bash

echo "Updating SOLR conf"

echo "Delete old configuration"
rm -rf ~/soft/solr-6.0.0/server/solr/whisky/

echo "Copy new configuration"
cp -r /Users/mkoz/coding/clojure/priceous/resources/solr/whisky ~/soft/solr-6.0.0/server/solr/
