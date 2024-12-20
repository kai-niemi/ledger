#!/bin/bash

# Configuration
########################

title="CockroachDB single region deployment (AZ)"
# CRDB release version
releaseversion="v24.2.4"
# Number of node instances in total including clients
nodes="7"
# Nodes hosting CRDB
crdbnodes="1-6"
# Array of client nodes (must match size of regions)
clientnodes="7"
# Array of regions localities (must match zone names)
regions="westeurope,westeurope,westeurope"
# AWS/GCE/AZ cloud (aws|gce)
cloud="azure"
# AWS/GCE/AZ region zones (must align with nodes size)
zones="\
westeurope,\
westeurope,\
westeurope,\
westeurope,\
westeurope,\
westeurope,\
westeurope"
# Machine type
machinetypes="Standard_D8_v4"

# Dry run mode
dryrun=on

# DO NOT EDIT BELOW THIS LINE
#############################

functionsdir="./common"
source "${functionsdir}/core_functions.sh"
main.sh