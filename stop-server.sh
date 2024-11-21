#!/bin/bash

pid=$(ps -ef | grep "java" | grep "ledger" | awk '{print $2}')

if [ -x ${pid} ]; then
   echo -e "No ledger.jar process found - is it running?"
   exit 1
fi

kill -TERM $pid
RETVAL=$?

echo -e "Stopped service (pid: $pid) $RETVAL"