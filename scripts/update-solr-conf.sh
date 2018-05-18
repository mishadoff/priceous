#!/bin/bash

echo "Updating SOLR conf"

echo "Delete old configuration"
rm -rf ~/soft/solr-7.3.1/server/solr/whisky/

echo "Copy new configuration"
cp -r ~/coding/priceous/resources/solr/whisky ~/soft/solr-7.3.1/server/solr/
