#!/bin/bash

echo "Updating SOLR conf"

echo "Delete old configuration"
rm -rf ~/soft/solr-6.4.1/server/solr/whisky/

echo "Copy new configuration"
cp -r ~/coding/clojure/priceous/resources/solr/whisky ~/soft/solr-6.4.1/server/solr/
