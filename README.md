# Poky

Poky is a work in progress. 

Poky is a bucketed key-value store built on [PostgreSQL](http://www.postgresql.org/) that speaks HTTP.
Poky uses [Clojure](http://clojure.org/) and the [Compojure](https://github.com/weavejester/compojure) web framework to
provide a REST interface over HTTP. Poky can be combined with [varnish](https://www.varnish-cache.org/) to speed up the REST API.

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

## HTTP Server Usage

Create a poky database and create the table:

    postgres=# CREATE DATABASE poky;
    postgres=# \c poky
    poky=# \i table.sql

To start a new instance of Poky:

    $ export DATABASE_URL=postgresql://postgres@pg.server.name:5432/poky
    $ ./scripts/run.http.text.sh -p 8081

OR

    $ ./scripts/run.http.text.sh --dsn "postgresql://postgres@pg.server.name:5432/poky" -p 8081

By default, the server listens on 8081. To change the listen port, use the -p
option.

For putting data in, both POST and PUT are accepted.

    $ curl -d"value" -H'Content-Type: application/text' -v -X PUT http://localhost:8081/bucket/key
    $ curl -d"\"json value\"" -H'Content-Type: application/json' -v -X PUT http://localhost:8081/bucket/key
    $ curl -d"value" -H'Content-Type: text/plain' -v -X PUT http://localhost:8081/bucket/key


    $ curl -d"value" -v -X POST http://localhost:8081/bucket/key

When putting data in, you should expect a status code of 200 if the request was
completed successfully.


When getting data out, use GET:

    $ curl -X GET http://localhost:8081/bucket/key
    value

Expect a status code of 200 and the data as the body.

## Dependencies

 * [daemonize](http://software.clapper.org/daemonize/)
 * [varnish](https://www.varnish-cache.org/)

## Contributors

 * [Logan Linn](https://github.com/loganlinn)
 * [Damon Snyder](https://github.com/drsnyder)
