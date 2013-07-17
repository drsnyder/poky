#!/bin/bash
PATH=$PATH:/sbin:/usr/sbin:/usr/local/sbin:/usr/local/bin
WORKING_DIR=$(cd $(dirname $0)/..; pwd)

. $WORKING_DIR/config/poky.defaults
. $WORKING_DIR/config/util

[ -e $WORKING_DIR/config/environment ] && . $WORKING_DIR/config/environment


export POKY_PID=$WORKING_DIR/run/poky.pid.$POKY_PORT

DAEMON_OPTS="-p $POKY_PORT"

function start() {
    setup $WORKING_DIR
    echo -n "Starting poky: "
    [ -e $POKY_PID ] && read PID < "$POKY_PID" > /dev/null
    kill -0 $PID 2> /dev/null
    alive=$?
    if [ $alive -eq 0 ]; then
        echo "poky is running. $0 stop first."
        exit 1
    fi

    daemonize -a -e $POKY_ERR_LOG -o $POKY_OUT_LOG -c $WORKING_DIR \
        -p $POKY_PID $POKY $DAEMON_OPTS
    retval=$?
    sleep 10

    # make sure the process is up
    read PID < "$POKY_PID" > /dev/null
    kill -0 $PID 2> /dev/null
    alive=$?

    if [ $retval -eq 0 -a $alive -eq 0 ]; then
        echo "started."
    else
        echo "failed. Check $POKY_ERR_LOG."
    fi
    return $retval
}

function stop() {
    echo -n "Stopping poky: "
    if [[ -e $POKY_PID ]]; then
        read PID < "$POKY_PID" > /dev/null

        kill -0 $PID 2> /dev/null
        alive=$?
        if [ $alive -ne 0 ]; then
          echo "poky isn't running"
          return 0
        fi

        kill -TERM $PID
        retval=$?
        if [[ $retval -eq 0 ]]; then
            sleep 3
            echo "shutdown poky"
        else
            echo "failed to shutdown poky"
        fi
    else
        echo "poky shutdown : no pidfile"
    fi
    echo

    return $retval
}

case "$1" in
    start)
        start
        ;;

    stop)
        stop
        ;;

    restart)
        stop
        start
        ;;

    *)
        echo "Usage: $0 start|stop|restart"
        ;;
esac

