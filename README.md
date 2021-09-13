# priceous [![Build Status](https://api.travis-ci.org/mishadoff/priceous.png)](https://travis-ci.org/mishadoff/priceous) [![Coverage Status](https://coveralls.io/repos/github/mishadoff/priceous/badge.svg?branch=master)](https://coveralls.io/github/mishadoff/priceous?branch=master)

Web-aggregator for whisky prices in ukrainian whisky shops

# Dev

```
lein uberjar
scripts/init-solr-volume.sh
cd ops && docker-compose up -d
```

# REPL

```
Start REPL...
Clojure 1.10.1
> (init)
```

## License

Copyright © 2019 mishadoff

Distributed under the Eclipse Public License either version 1.0