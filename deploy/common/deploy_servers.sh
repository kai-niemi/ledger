#!/bin/bash

fn_failcheck roachprod run ${CLUSTER}:${clientnodes} "mkdir -p config"
fn_failcheck roachprod put ${CLUSTER}:${clientnodes} ../target/ledger.jar
fn_failcheck roachprod put ${CLUSTER}:${clientnodes} ../config/application-default.yml config/
fn_failcheck roachprod put ${CLUSTER}:${clientnodes} scripts/run.sh
fn_failcheck roachprod put ${CLUSTER}:${clientnodes} scripts/start.sh
fn_failcheck roachprod put ${CLUSTER}:${clientnodes} scripts/stop.sh

fn_echo_info_nl "B: All client files deployed"
