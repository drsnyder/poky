#!/bin/bash
PATH=$PATH:/sbin:/usr/local/sbin:/usr/local/bin
WORKING_DIR=$(cd $(dirname $0)/..; pwd)

. $WORKING_DIR/config/varnish.defaults
. $WORKING_DIR/config/util


[ -e $WORKING_DIR/config/environment ] && . $WORKING_DIR/config/environment


DAEMON_OPTS="-a ${VARNISH_LISTEN_ADDRESS}:${VARNISH_LISTEN_PORT} \
             -f ${VARNISH_VCL_CONF} \
             -T ${VARNISH_ADMIN_LISTEN_ADDRESS}:${VARNISH_ADMIN_LISTEN_PORT} \
             -t ${VARNISH_TTL} \
             -w ${VARNISH_MIN_THREADS},${VARNISH_MAX_THREADS},${VARNISH_THREAD_TIMEOUT} \
             -u $VARNISHD_USER -g $VARNISHD_GROUP \
             -s "${VARNISH_STORAGE_TYPE},${VARNISH_STORAGE_SIZE}" \
             -p thread_pools=${VARNISH_THREAD_POOLS} \
             -p thread_pool_add_delay=${VARNISH_THREAD_POOL_ADD_DELAY} \
             -p listen_depth=${VARNISH_LISTEN_DEPTH} \
             -p ban_lurker_sleep=${BAN_LURKER_SLEEP} \
             -p lru_interval=${LRU_INTERVAL} \
             -p sess_workspace=${SESS_WORKSPACE} \
             -P $VARNISHD_PID \
             -n $WORKING_DIR/run/"

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

        kill -0 $PID 2> /dev/null
        alive=$?
        if [ $alive -ne 0 ]; then
          echo "varnishd isn't running"
          return 0
        fi

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
