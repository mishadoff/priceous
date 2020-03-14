# Architecture

## Flow

1. Lightweight Spider
2. Enrichment Spider
3. Postgres Writer
4. SOLR Indexer

## Lightweight Spider

The lightweight spider goals is to quickly inspect the site
and detect products. Most common use-case is to process 
pages of results without drilling into product details.

The result of such processing could be either 
a link for product for further enrichment, or price/sale information.

Lightweight Spider is meant to run fast and 
could be triggered several times per day to reflect
relevant information about product.

## Enrichment Spider

The enrichment spider is using information 
from the lightweight spider and collects additional information 
about the product. It's slow and therefore should be triggered rarely.

## Postgres Writer

Information collected by the spiders is written into postgres
in semi-structured way. 

## SOLR Indexer

After scrapping is finished, solr retrieves latest and greatest
results from postgres and reindex the data which powers site. 
