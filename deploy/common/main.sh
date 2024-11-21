#!/bin/bash

core_util.sh

case "$OSTYPE" in
  darwin*)
    rootdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    selfname="$(basename "$(test -L "$0" && readlink "$0" || echo "$0")")"
    ;;
  *)
    rootdir="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
    selfname="$(basename "$(readlink -f "${BASH_SOURCE[0]}")")"
    ;;
esac

fn_echo_header
{
	echo -e "${lightblue}Cluster id:\t\t${default}$CLUSTER"
	echo -e "${lightblue}Node count:\t\t${default}$nodes"
	echo -e "${lightblue}CRDB nodes:\t\t${default}$crdbnodes"
	echo -e "${lightblue}CRDB version:\t\t${default}$releaseversion"
	echo -e "${lightblue}Client nodes:\t\t${default}${clientnodes}"
	echo -e "${lightblue}Cloud:\t\t${default}$cloud"
	echo -e "${lightblue}Machine types:\t\t${default}$machinetypes"
	echo -e "${lightblue}Regions:\t\t${default}$regions"
	echo -e "${lightblue}Zones:\t\t${default}$zones"
	echo -e "${lightblue}Dryrun:\t\t${default}$dryrun"
} | column -s $'\t' -t

if [ "$(whoami)" == "root" ]; then
    fn_echo_warning "Do NOT run as root!"
    exit 1
fi

if [ -z "${CLUSTER}" ]; then
  fn_echo_warning "No \$CLUSTER id variable set!"
  echo "Use: export CLUSTER='your-cluster-id'"
  exit 1
fi

fn_split_array ${clientnodes}
clientnodes_arr=("${OUT[@]}")

fn_split_array ${crdbnodes}
crdbnodes_arr=("${OUT[@]}")

IFS=',' read -ra regions_arr <<< "$regions"

IFS=',' read -ra zones_arr <<< "$zones"

if [ "${dryrun}" == "on" ]; then
  echo "Nodes: (${crdbnodes})"
  for value in "${crdbnodes_arr[@]}" ; do
  echo -e "${lightyellow}$value${default}"
  done

  echo "Clients: (${clientnodes})"
  for value in "${clientnodes_arr[@]}" ; do
  echo -e "${lightyellow}$value${default}"
  done

  echo "Regions: (${regions})"
  for value in "${regions_arr[@]}" ; do
  echo -e "${lightyellow}$value${default}"
  done

  echo "Zones: (${zones})"
  for value in "${zones_arr[@]}" ; do
  echo -e "${lightyellow}$value${default}"
  done
fi

if fn_prompt_yes_no "A: Create CockroachDB Cluster?" Y; then
  create_cluster.sh
fi

if fn_prompt_yes_no "B: Deploy server to client machines?" Y; then
  deploy_servers.sh
fi

if fn_prompt_yes_no "C: Print URLs?" Y; then
  show_urls.sh
fi

