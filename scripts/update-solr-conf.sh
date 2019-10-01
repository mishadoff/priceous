#!/bin/bash

echo "Updating SOLR conf"

SOLR_VERSION="8.2.0"

echo "Delete old configuration"
rm -rf ~/soft/solr-$SOLR_VERSION/server/solr/whisky/

echo "Copy new configuration"
cp -r ~/coding/clojure/priceous/resources/solr/whisky ~/soft/solr-$SOLR_VERSION/server/solr/
