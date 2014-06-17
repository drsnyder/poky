#!/bin/bash
# Create a poky database using a given scheme. The partitioned scheme is recommeneded.
# Example:
# ./scripts/create-poky-database.sh postgres dbhost poky partitioned

function usage() {
  echo "$0 <USER> <HOST> <DB> <flat|partitioned>"
  exit 1
}

USER=$1
HOST=$2
DB=$3
SCHEME=$4

if [[ -z $USER || -z $HOST || -z $DB || -z $SCHEME ]]; then
  usage
fi

if [[ ! -e sql/$SCHEME ]]; then
  echo "Error, sql/$SCHEME does not exist. Did you mean 'flat' or 'partitioned' for the scheme?"
  usage
  exit 1
fi

psql -U$USER -h$HOST -c "CREATE DATABASE $DB;"

psql -U$USER -h$HOST $DB < sql/$SCHEME/poky.sql

psql -U$USER -h$HOST $DB < sql/shared/stored_procedures.sql
psql -U$USER -h$HOST $DB < sql/shared/triggers.sql


psql -U$USER -h$HOST $DB < sql/$SCHEME/stored_procedures.sql
psql -U$USER -h$HOST $DB < sql/$SCHEME/triggers.sql
