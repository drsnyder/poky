# Poky

A key-value store built on [PostgreSQL](http://www.postgresql.org/) that speaks the
[memcached](https://github.com/memcached/memcached/blob/master/doc/protocol.txt)
protocol. Poky is built using Clojure on top of the asynchronous communication
framework [Aleph](https://github.com/ztellman/aleph).

## Rationale

Many organizations have a significant investment in, and confidence 
supporting PostgreSQL. They have come to rely on its proven architecture and its
reputation for reliability and data integrity. Poky is an attempt to augment
this investment by providing a simple key-value store using an API (memcached) that already
exists for most popular languages. 

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

    $ export POKY_SUBNAME=pg.server.name:5432/poky
    $ lein trampoline run

By default, the server listens on 11219. To change the listen port, use the -p
option.

## License

Copyright (C) 2012 Damon Snyder 

Distributed under the Eclipse Public License, the same as Clojure.
