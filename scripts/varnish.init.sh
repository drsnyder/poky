#!/bin/bash
PATH=$PATH:/sbin:/usr/local/sbin:/usr/local/bin
WORKING_DIR=$(cd $(dirname $0)/..; pwd)

. $WORKING_DIR/config/varnish.defaults
. $WORKING_DIR/config/util


[ -e $WORKING_DIR/config/environment ] && . $WORKING_DIR/config/environment


function start() {
    # Open files (usually 1024, which is way too small for varnish)
    ulimit -n ${NFILES:-8192} 2> /dev/null
    [ $? -ne 0 ] && echo "!! ulimit -n failed. Check your os settings for file-max. !!"

    # Varnish wants shared memory log in memory. 
    ulimit -l ${SHMEM:-82000} 2> /dev/null
    [ $? -ne 0 ] && echo "!! ulimit -l failed. Check your os settings for file-max. !!"

    setup $WORKING_DIR
    echo -n "Starting varnishd: "
    touch $VARNISHD_PID
    $VARNISHD $DAEMON_OPTS
    retval=$?
    if [[ $retval -eq 0 ]]; then
        echo "started varnishd"
    else
        echo "failed to start varnishd"
    fi
    echo

    return $retval
}

function stop() {
    echo -n "Stopping varnishd: "
    if [[ -e $VARNISHD_PID ]]; then
        read PID < "$VARNISHD_PID" > /dev/null
        kill $PID
        retval=$?
        if [[ $retval -eq 0 ]]; then
            echo "shutdown varnishd"
        else
            echo "failed to shutdown varnishd"
        fi
    else
        echo "varnishd shutdown : no pidfile"
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
