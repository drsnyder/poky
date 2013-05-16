#!/bin/bash

POKY_HOME=$(cd $(dirname $0)/..; pwd)
POKY_JAR=$POKY_HOME/target/poky-standalone.jar
: ${JMX_PORT:="9191"}

[ -e $POKY_HOME/config/environment ] && . $POKY_HOME/config/environment

if [ ! -e $POKY_JAR ]; then
    $(cd $POKY_HOME && lein clean && lein uberjar)
fi

trap 'echo "runner shutting down"; kill $(jobs -p); exit;' TERM

java -Xmx1024m -server -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=$JMX_PORT \
    -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false \
        -cp $POKY_JAR clojure.main -m poky.protocol.http.jdbc.text.main $* &

wait
