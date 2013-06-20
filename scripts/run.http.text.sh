#!/bin/bash

POKY_HOME=$(cd $(dirname $0)/..; pwd)
POKY_JAR=$POKY_HOME/target/poky-standalone.jar
: ${JMX_PORT:="9291"}

[ -e $POKY_HOME/config/environment ] && . $POKY_HOME/config/environment

if [ ! -e $POKY_JAR ]; then
    echo "Rebuilding the jar."
    $(cd $POKY_HOME && lein clean && lein uberjar)
    echo "Done rebuilding."
fi

trap 'echo "runner shutting down"; kill $(jobs -p); exit;' TERM

java -Xmx1024m -server -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=$JMX_PORT \
    -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false \
    -Dpoky.home=$POKY_HOME \
        -cp $POKY_JAR clojure.main -m poky.protocol.http.main $* &

wait
