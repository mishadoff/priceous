#!/bin/bash

# Detects the location of the bash script no matter where it run from
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# Temp solr data directory
TEMP_DIR="temp/whisky"

echo "Init SOLR volume temp dirfor docker compose"

echo "Delete old configuration"
rm -rf $DIR/../$TEMP_DIR

echo "Copy new configuration"
cp -r $DIR/../resources/solr/whisky $DIR/../$TEMP_DIR