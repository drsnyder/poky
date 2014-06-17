#!/bin/bash
# Create a bucket in the partitioned schema. This is not necessary when using the flat schema.
# Example:
# ./scripts/create-bucket.sh postgres dbhost poky test_bucket

function usage() {
  echo "$0 <USER> <HOST> <DB> <my_data_bucket>"
  exit 1
}

USER=$1
HOST=$2
DB=$3
BUCKET=$4

if [[ -z $USER || -z $HOST || -z $DB || -z $BUCKET ]]; then
  usage
fi


psql -U$USER -h$HOST $DB -c "SELECT create_bucket_partition('$BUCKET'::text);"
if [[ $? -eq 0 ]]; then
  echo "Created bucket $BUCKET."
fi
