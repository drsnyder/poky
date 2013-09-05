#!/bin/bash
# ./scripts/get-benchmark-data.sh dbhost database table 1000 
USER=postgres
HOST=$1
DATABASE=$2
BUCKET=$3
COUNT=$4
TABLE=poky
psql -F"," -A -t -U$USER -h$HOST $DATABASE -c "SELECT '$BUCKET', key, date_trunc('seconds',modified_at) FROM $TABLE WHERE bucket = '$BUCKET' ORDER BY random() LIMIT $COUNT;"
