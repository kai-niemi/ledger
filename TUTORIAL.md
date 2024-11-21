# Multi-region Tutorial

This page describes how to setup a complete multi-region demo or load test.

## Create a CockroachDB cluster

This tutorial does not cover CockroachDB cluster setups, see [start a local cluster](https://www.cockroachlabs.com/docs/v24.2/start-a-local-cluster) for that.

Assume you have a 9 node secure cluster with 16-32 vCPUs per node across 3 different regions.

| Region       | Primary | Secondary | Zones                                     | Nodes | Clients |
|--------------|:--------|:----------|-------------------------------------------|-------|:--------|
| us-east-1    |         |           | us-east-1a,us-east-1b,us-east-1c          | 1-3   | 10      |
| eu-central-1 | x       |           | eu-central-1a,eu-central-1b,eu-central-1c | 4-6   | 11      |
| eu-north-1   |         | x         | eu-north-1a,eu-north-1b,eu-north-1c       | 7-9   | 12      |

The _Nodes_ column refers to the CockroachDB node numbers. The _Clients_ column refers to the VMs/machines
hosting the ledger server and a CockroachDB binary and HAProxy load balancer in case it's a self-hosted cluster.
For CockroachDB Cloud, you don't need the binary or HAProxy since its built-in. The client(s) can be deployed
in any of the zones, or all of them.

## Deploy the clients

Assuming each client is already prepared with the needed dependencies in prerequisites.

### Client 10

Create the database:

    ./cockroach sql --certs-dir=certs --host=<ip> -e "CREATE DATABASE ledger; ALTER ROLE root WITH PASSWORD 'cockroach'"

If self-hosted, generate a haproxy config and start:

    ./cockroach gen haproxy --certs-dir=certs --host <ip> --external --locality=region=us-east-1
    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

### Client 11

If self-hosted, generate a haproxy config and start:

    ./cockroach gen haproxy --certs-dir=certs --host <ip> --external --locality=region=eu-central-1
    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

### Client 12

If self-hosted, generate a haproxy config and start:

    ./cockroach gen haproxy --certs-dir=certs --host <ip> --external --locality=region=eu-north-1
    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

## Start the servers

### Client 10

In a setup like this, you may want to enable the multi-region capabilities including:

- Regions (primary and secondary)
- Table localities (global and regional-by-row)
- Survival goal (from zone to region)

Start the server shell and execute:

    java -jar ledger.jar
    :$ build-account-plan

Ledger is optimized to run efficiently in a multi-region CockroachDB cluster. It can add regions,
table localities and survival goals automatically. This will enable all these features in one go:

    :$ apply-multi-region

This command will add actual database regions, set the survival goal to `REGION` and also set
table localities including `regional-by-row` with pinning based on the configured application
regions and cities. Note that this can be a heavy operation, so it may take a while to run 
and for the cluster to rebalance the data as necessary.

Next, start some workloads:

    :$ transfer-funds 
    :$ read-balance

You should notice that it picks the cities of the local region (us-east-1).

### Client 11

Start the server shell and some workloads:

    java -jar ledger.jar 
    :$ transfer-funds 
    :$ read-balance

You should notice that it picks the cities of the local region (eu-central-1).

### Client 12

Start the server shell and some workloads:

    java -jar ledger.jar 
    :$ transfer-funds 
    :$ read-balance

You should notice that it picks the cities of the local region (eu-north-1).

## Conclusions

Expected observations and application behavior in this setup.
                                          
### Performance

You should observe local read and write latencies for transactions towards the 
in-region accounts since these accounts a geo-partitioned (pinned) to the
local regions. By the same token, you should observe higher latencies when
transferring funds across EU accounts from a US client and vice versa.

For reference, a 3x16 vCPU single-region cluster with a 2GB dataset can 
provide approximately:

- 60K QPS at P99 <1ms for point balance reads.
- 40K QPS at P99 20ms P99 for read-write transactions (transfers).

### Survival

With regional survival, you should be able to observe forward progress also
during a full regional outage except for the data pinned to the downed region.

### Consistency

With serializable isolation you will always observe consistent outcomes 
and zero balance sums per city (the checksum). With read committed isolation,
given that locking is enabled, you will also observe only correct outcomes.

### Locality

With table localities set to regional-by-row, you will observe predictable
read and write latencies in the local regions. Data domiciling is partly 
constrained since non-voting replicas can be placed outside of the row's 
home region. To avoid that, you'd need to use super regions which is out
of scope for this tutorial.
