#!/bin/bash

if [ -z "${CLUSTER}" ]; then
  fn_echo_warning "No \$CLUSTER id variable set!"
  export CLUSTER="your-cluster-id"
fi

for c in $clientnodes_arr
do
  ip=$(roachprod ip $CLUSTER:$c --external)
  echo -e "${lightyellow}open http://$ip:9090${default}"
  echo -e "${lightyellow}roachprod run $CLUSTER:$c${default}"
done

for c in $crdbnodes_arr
do
  echo -e "${lightyellow}roachprod admin --open --ips $CLUSTER:$c${default}"
done
