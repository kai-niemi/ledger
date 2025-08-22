#!/bin/bash
# Script for setting up a multi-region Ledger cluster using roachprod in either AWS or GCE.

# Configuration
########################

title="CockroachDB 3-region EU deployment"
# CRDB release version
releaseversion="v25.3.0"
# AWS/GCE cloud (aws|gce)
cloud="aws"
# Number of node instances in total including clients
nodes="12"
# Nodes hosting CRDB
crdbnodes="1-9"
# Array of client nodes (must match size of regions)
clientnodes="10-12"
# Array of regions localities (must match zone names)
regions="eu-west-1,eu-central-1,eu-west-2"
# AWS/GCE region zones (must align with nodes count)
zones="\
eu-west-1a,\
eu-west-1b,\
eu-west-1c,\
eu-central-1a,\
eu-central-1b,\
eu-central-1c,\
eu-west-2a,\
eu-west-2b,\
eu-west-2c,\
eu-west-1a,\
eu-central-1a,\
eu-west-2a"
# AWS/GCE machine types
machinetypes="m6i.2xlarge"

# Dry run mode
dryrun=off

# DO NOT EDIT BELOW THIS LINE
#############################

functionsdir="./common"
source "${functionsdir}/core_functions.sh"
main.sh