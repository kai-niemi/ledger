#!/bin/bash
# Script for setting up a single-region Ledger cluster using roachprod in either AWS or GCE.

# Configuration
########################

title="CockroachDB single region deployment (GCE)"
# CRDB release version
releaseversion="v24.2.4"
# Number of node instances in total including clients
nodes="4"
# Nodes hosting CRDB
crdbnodes="1-3"
# Array of client nodes (must match size of regions)
clientnodes="4"
# Array of regions localities (must match zone names)
regions="us-east4"
# AWS/GCE cloud (aws|gce)
cloud="gce"
# AWS/GCE region zones (must align with nodes size)
zones="\
us-east4-a,\
us-east4-a,\
us-east4-a,\
us-east4-b,\
us-east4-b,\
us-east4-b,\
us-east4-c,\
us-east4-c,\
us-east4-c,\
us-east4-a,\
us-east4-b,\
us-east4-c"
# AWS/GCE machine types
machinetypes="n2-standard-8"

# Dry run mode
dryrun=on

# DO NOT EDIT BELOW THIS LINE
#############################

functionsdir="./common"
source "${functionsdir}/core_functions.sh"
main.sh