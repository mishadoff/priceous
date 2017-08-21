# Data Coverage

## Rationale

To give better understanding of scrapper quality, we provide a data coverage
metrics, which shows how many fields in document are present in data result.

For example, if you compare two documents:

DOC1

```
{:provider "Goodwine" 
 :name     "Виски Glenfiddich 12 yo"
 :price    750.00}
```

DOC2

```
{:provider    "Goodwine" 
 :name        "Виски Glenfiddich 12 yo"
 :price       750.00
 :volume      0.7
 :alcohol     40.0
 :description "Яблоко, ваниль и много солода"
 :country     "Шотландия"
 :excise      true
 :trusted     true}
```

Obviously, document 2 has more information, hence a better data coverage.

## Algorithm

The algorithm for data coverage is based per document. Total data coverage
for provider will be average across all documents.

To calculate coverage for a document we give 1 point if field is present, and 0 if not.
Then just divide sum of all points to total number of fields.

This will be very raw score, and as a step forward we just provide weights
for some fields, for example `name` and `price` are more important than `description` and `country`
