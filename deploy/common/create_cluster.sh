#!/bin/bash

fn_create_cluster() {
  if [ "${cloud}" = "aws" ]; then
    fn_failcheck roachprod create $CLUSTER --clouds=aws --aws-machine-type-ssd=${machinetypes} --geo --local-ssd --nodes=${nodes} --aws-zones=${zones} --aws-profile crl-revenue --aws-config ~/rev.json
  elif [ "${cloud}" = "gce" ]; then
    fn_failcheck roachprod create $CLUSTER --clouds=gce --gce-machine-type=${machinetypes} --geo --local-ssd --nodes=${nodes} --gce-zones=${zones} --aws-profile crl-revenue --aws-config ~/rev.json
  else
    fn_failcheck roachprod create $CLUSTER --clouds=azure --azure-machine-type=${machinetypes} --geo --local-ssd --nodes=${nodes} --azure-locations=${zones} --aws-profile crl-revenue --aws-config ~/rev.json
  fi
}

fn_stage_cluster() {
  fn_echo_info_nl "Stage binaries $releaseversion"

  fn_failcheck roachprod stage $CLUSTER release $releaseversion
}

fn_start_cluster() {
  fn_echo_info_nl "Start CockroachDB nodes $crdbnodes"

  fn_failcheck roachprod start $CLUSTER:$crdbnodes
  fn_failcheck roachprod admin --open --ips $CLUSTER:1
}

fn_stage_clients() {
  fn_echo_info_nl "Stage clients ${CLUSTER}:$clientnodes"

  i=0;
  for c in $clientnodes_arr
  do
    region=${regions_arr[$i]}
    i=($i+1)

    echo -e "Client: $c Region: $region"

    fn_failcheck roachprod run ${CLUSTER}:$c "./cockroach gen haproxy --certs-dir=certs --host $(roachprod ip $CLUSTER:1 --external) --locality=region=$region"
  done

  fn_failcheck roachprod run ${CLUSTER}:$clientnodes 'sudo apt-get -qq update'
  fn_failcheck roachprod run ${CLUSTER}:$clientnodes 'sudo apt-get -qq install -y openjdk-21-jre-headless htop dstat haproxy'
  fn_failcheck roachprod run ${CLUSTER}:$clientnodes 'nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &'
}

fn_create_db() {
fn_echo_info_nl "Creating database via $CLUSTER:1"

fn_failcheck roachprod run $CLUSTER:1 <<EOF
./cockroach sql --certs-dir=certs --host=`roachprod ip $CLUSTER:1` -e "CREATE DATABASE ledger; ALTER ROLE root WITH PASSWORD 'cockroach'"
EOF
}

##################################################################

if fn_prompt_yes_no "A 1/5: Create new cluster?" Y; then
fn_create_cluster
fi

if fn_prompt_yes_no "A 2/5: Stage cluster?" Y; then
fn_stage_cluster
fi

if fn_prompt_yes_no "A 3/5: Start cluster?" Y; then
fn_start_cluster
fi

if fn_prompt_yes_no "A 4/5: Stage clients?" Y; then
fn_stage_clients
fi

if fn_prompt_yes_no "A 5/5: Create DB?" Y; then
fn_create_db
fi
