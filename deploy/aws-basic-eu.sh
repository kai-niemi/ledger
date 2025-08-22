#!/bin/bash

# Configuration
########################

title="CockroachDB single region deployment (AWS)"
# CRDB release version
releaseversion="v25.3.0"
# AWS/GCE cloud (aws|gce|az)
cloud="aws"
# Number of node instances in total including clients
nodes="4"
# Nodes hosting CRDB
crdbnodes="1-3"
# Nodes hosting client
clientnodes="4"
# Region localities (must match zone names)
regions="eu-central-1"
# Region zones (must align with total node count)
zones="\
eu-central-1a,\
eu-central-1b,\
eu-central-1c,\
eu-central-1a"
# AWS/GCE machine types
# https://aws.amazon.com/ec2/instance-types/m6i/
machinetypes="m6i.4xlarge"

# Dry run mode
dryrun=off

# DO NOT EDIT BELOW THIS LINE
#############################

functionsdir="./common"
source "${functionsdir}/core_functions.sh"
main.sh