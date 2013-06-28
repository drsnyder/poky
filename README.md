[![Build Status](https://travis-ci.org/drsnyder/poky.png)](https://travis-ci.org/drsnyder/poky)


# Poky

Poky is a bucketed key-value store built on [PostgreSQL](http://www.postgresql.org/) that speaks HTTP.
Poky uses [Clojure](http://clojure.org/) and the [Compojure](https://github.com/weavejester/compojure) web framework to
provide a REST interface over HTTP. Poky can be combined with [varnish](https://www.varnish-cache.org/) to speed up the REST API.

## Rationale

Many organizations have a significant investment in, and confidence 
supporting PostgreSQL. They have come to rely on its proven architecture and its
reputation for reliability and data integrity. Poky is an attempt to augment
this investment by providing a simple key-value store using a REST API. 

Some datasets are key-value in nature but are too big to practically or cost
effectively fit in-memory using redis or memcached. Poky provides an alternative
where the data set size could be much larger and bounded only by
PostgreSQL's maximum table size (currently 32TB).

Additionally, some types of data, though they may be derived, are resource
intensive to generate. In Poky, once the data has been committed to disk, it will
remain so even in the event of power loss, crashes, or errors which reduces or
eliminates the possibility of a costly regeneration of the data.


## Deploy Poky as a Service

Before deploying Poky as a service, confirm or edit the settings in the
capistrano configuration file config/deploy.rb. You will be primarily concerned
that the hostnames are configured correctly for your environment.

Confirm that you have varnish and daemonize installed on the target system. See
the dependencies section below.

Next, deploy to the environment of choice:

    cap development deploy

Other configuration files of interest:

 * config/defaults.vcl: the default varnish configuration.
 * config/deploy.rb: the capistrano configuration.
 * config/poky.defaults: default settings for the poky service.
 * config/varnish.defaults: default settings for varnishd.

## HTTP Server Usage

Create a poky database and create the table:

    postgres=# CREATE DATABASE poky;
    postgres=# \c poky
    poky=# \i sql/table.sql
    poky=# \i sql/triggers.sql

To start a new instance of Poky:

    $ export DATABASE_URL=postgresql://postgres@pg.server.name:5432/poky
    $ ./scripts/run.http.text.sh -p 8081

OR

    $ ./scripts/run.http.text.sh --dsn "postgresql://postgres@pg.server.name:5432/poky" -p 8081

By default, the server listens on 8081. To change the listen port, use the -p
option.

## API

For putting data in, both POST and PUT are accepted.

    $ curl -d"value" -H'Content-Type: application/text' -v -X PUT http://localhost:8081/kv/bucket/key
    $ curl -d"\"json value\"" -H'Content-Type: application/json' -v -X PUT http://localhost:8081/kv/bucket/key
    $ curl -d"value" -H'Content-Type: text/plain' -v -X PUT http://localhost:8081/kv/bucket/key


    $ curl -d"value" -v -X POST http://localhost:8081/kv/bucket/key
    $ curl -d"value" -H "If-Unmodified-Since: Tue, 04 Jun 2013 03:01:31 GMT" -v -X POST http://localhost:8081/kv/bucket/key

When putting data in, you should expect a status code of 200 if the request was
completed successfully. If a newer version has already been committed the
request will be rejected with a 412 status code. See the Consistency section
below.

When getting data out, use GET:

    $ curl -X GET http://localhost:8081/kv/bucket/key
    value

For additional documentation:

    $ curl -X GET http://localhost:8081/help

## Working in the REPL

For logging to work properly, add a profile to your ~/lein/profiles.clj that
sets the Java define `poky.home`:

    {:poky {:jvm-opts ["-Dpoky.home=/path/to/poky"]}}

This will initialize the logging to write to /path/to/poky/log/poky.log. When
you start the repl, start it with:

    lein with-profile dev,poky repl

Failing to do so may result in some exceptions being thrown when the repl is
started.


## Consistency

Poky observes the If-Unmodified-Since header. If this header is present in a
PUT/POST request the request will only be accepted if the timestamp supplied is
greater than or equal to the current `modified_at`. If no If-Unmodified-Since is presented,
the request will be unconditionally accepted and the `modified_at` current timestamp
will be left unchanged.

Use caution when omitting the If-Unmodified-Since header. If it's possible
that two requests may attempt to update an object simultaneous the system can
only guarantee that the most request request is accepted if the If-Unmodified-Since
header is provided.

For GET requests, Poky observes the If-Match header. If the If-Match header is
supplied with the GET request and matches the `modified_at` timestamp of the
entity being requested the operation will be performed as if the If-Match
header did not exist. If the value supplied in the If-Match header does not
match the `modified_at` timestamp the request will be rejected.

The If-Match header should be a quoted RFC 1123 date string.

In both scenarios above if the request is rejected the status code will be set
to 412 (Precondition Failed).

## Dependencies

Poky was developed and tested on PostgreSQL 9.2.

 * [PostgreSQL 9.2](http://www.postgresql.org/)

The following dependencies are required to deploy (or run) poky as a service.

 * [daemonize](http://software.clapper.org/daemonize/)
 * [varnish](https://www.varnish-cache.org/)
 * [capistrano](https://github.com/capistrano/capistrano)

## Contributors

 * [Logan Linn](https://github.com/loganlinn)
 * [Damon Snyder](https://github.com/drsnyder)
