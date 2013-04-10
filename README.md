# Poky

Poky is a work in progress. 

Poky is a key-value store built on [PostgreSQL](http://www.postgresql.org/) that speaks HTTP. Poky uses Clojure and the web framework
Compojure to provide an HTTP head.

Experimental WIP: The [memcached](https://github.com/memcached/memcached/blob/master/doc/protocol.txt)
protocol may also be supported on top of the asynchronous communication
framework [Aleph](https://github.com/ztellman/aleph).

## Rationale

Many organizations have a significant investment in, and confidence 
supporting PostgreSQL. They have come to rely on its proven architecture and its
reputation for reliability and data integrity. Poky is an attempt to augment
this investment by providing a simple key-value store using a REST API. 

Additionally, some datasets are key-value in nature but are too big to
practically or cost effectively fit in-memory using redis or memcached. Poky provides an
alternative where the data set size could be much larger and bounded only by
PostgreSQL's maximum table size (currently 32TB).

## Usage

Create a poky database and create the table:

    postgres=# CREATE DATABASE poky;
    postgres=# \c poky
    poky=# \i table.sql

To start a new instance of Poky:

    $ export DATABASE_URL=pg.server.name:5432/poky
    $ ./scripts/run.http.text.sh -p 8081
    
OR

    $ ./scripts/run.http.text.sh --dsn "postgresql://postgres@pg.server.name/poky" -p 8081

Getting data in and out:

    # putting data in
    $ curl -d"value" -H'Content-Type: application/text' -v -X PUT http://localhost:8080/key
    $ curl -d"\"value\"" -H'Content-Type: application/json' -v -X PUT http://localhost:8080/key
    $ curl -d"value" -H'Content-Type: text/plain' -v -X PUT http://localhost:8080/key

    # getting data out
    $ curl -X GET http://localhost:8080/key
    {"key":"value"}
    $ curl -H'Accept: text/plain' -X GET http://localhost:8080/key
    value


By default, the server listens on 8080. To change the listen port, use the -p
option.

## License

Copyright (C) 2013 Damon Snyder 

Distributed under the Eclipse Public License, the same as Clojure.
