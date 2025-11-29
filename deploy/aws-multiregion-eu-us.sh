#!/bin/bash

# Configuration
########################

title="CockroachDB 3-region EU-US deployment"
# CRDB release version
releaseversion="v25.4.0"
# AWS/GCE cloud (aws|gce)
cloud="aws"
# Number of node instances in total including clients
nodes="12"
# Nodes hosting CRDB
crdbnodes="1-9"
# Array of client nodes (must match size of regions)
clientnodes="10-12"
# Array of regions localities (must match zone names)
regions="us-east-1,eu-central-1,eu-west-1"
# AWS/GCE region zones (must align with nodes count)
zones="\
us-east-1a,\
us-east-1b,\
us-east-1c,\
eu-central-1a,\
eu-central-1b,\
eu-central-1c,\
eu-west-1a,\
eu-west-1b,\
eu-west-1c,\
us-east-1a,\
eu-central-1a,\
eu-west-1a"
# AWS/GCE machine types
machinetypes="m6i.xlarge"

# Dry run mode
dryrun=on

# DO NOT EDIT BELOW THIS LINE
#############################

functionsdir="./common"
source "${functionsdir}/core_functions.sh"
main.sh