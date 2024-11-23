# Multi-region Tutorial

This page describes how to set up a complete multi-region demo or large scale load test.

## Create a CockroachDB cluster

This tutorial does not cover any CockroachDB cluster setups, see:
- https://www.cockroachlabs.com/docs/stable/manual-deployment
- https://www.cockroachlabs.com/docs/cockroachcloud

Let's assume you have a 9 node cluster with 8-32 vCPUs per node across 3 different regions, as follows.

| Region       | Primary | Secondary | Zones                                     | Nodes | Clients |
|--------------|:--------|:----------|-------------------------------------------|-------|:--------|
| us-east-1    |         |           | us-east-1a,us-east-1b,us-east-1c          | 1-3   | 10      |
| eu-central-1 | x       |           | eu-central-1a,eu-central-1b,eu-central-1c | 4-6   | 11      |
| eu-north-1   |         | x         | eu-north-1a,eu-north-1b,eu-north-1c       | 7-9   | 12      |

The _Nodes_ column refers to the CockroachDB node numbers. The _Clients_ column refers to the VMs/machines
hosting the ledger server, a CockroachDB binary and HAProxy load balancer in case it's a self-hosted cluster.

For CockroachDB Cloud, you don't need the binary or HAProxy. The client(s) can be deployed
in any of the region zones, or all of them. 

## Deploy the clients

Assuming each client is already prepared with the required dependencies:

- CockroachDB binary
- Ledger JAR file
- JDK21

The rest of this tutorial assumes a self-hosted, secure CockroachDB cluster.

### Client 10

Use this client to create the database:

    ./cockroach sql --certs-dir=certs --host=<ip> -e "CREATE DATABASE ledger; ALTER ROLE root WITH PASSWORD 'cockroach'"

Generate a haproxy config file for `us-east-1` and start it:

    ./cockroach gen haproxy --certs-dir=certs --host <ip> --external --locality=region=us-east-1
    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

### Client 11

Generate a haproxy config file for `eu-central-1` and start it:

    ./cockroach gen haproxy --certs-dir=certs --host <ip> --external --locality=region=eu-central-1
    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

### Client 12

Generate a haproxy config file for `eu-north-1` and start it:

    ./cockroach gen haproxy --certs-dir=certs --host <ip> --external --locality=region=eu-north-1
    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

## Start the servers

### Client 10

In a setup like this, you want to enable the multi-region capabilities including:

- Adding regions (primary and secondary)
- Table localities (global and regional-by-row)
- Survival goal (from zone to region)

Start the server and build the account plan:

    java -jar ledger.jar
    :$ build-account-plan

Ledger is optimized both for single and multi-region CockroachDB deployments. It can add regions,
table localities and survival goals automatically by DDL statements. 

This command will enable all these features in one go:

    :$ apply-multi-region

It will add the database regions, set the survival goal to `REGION` and also set
table localities to `regional-by-row`. This will make the table rows home region
to be determined by the value of its city column (using the `region` computed 
column as partition key). 

Note that this can be a heavy operation so it may take a while to run and 
for the cluster then re-balance the data as necessary. You can observe this
activity in the DB Console.

Next, start some workloads for this region, for example:

    :$ transfer-funds 
    :$ read-balance

You should notice that it picks the cities for the local region (us-east-1).

As a later exercise, you can change the survival goal back to `zone` (the default) 
and observe its effect on forward progress.

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

Expected application behavior in this setup.
                                          
### Performance

You should observe local read and write latencies for all transactions towards 
the "in-region" accounts, since these accounts have been optimized for access
from the local regions. By the same token, you should observe higher latencies when
for example transferring funds across EU accounts from a US client and vice versa.

For reference, a 3x16 vCPU single-region cluster with a 2GB dataset should 
provide approximately:

- 60K QPS at P99 <1ms for point balance reads.
- 40K QPS at P99 20ms P99 for read-write transactions (transfers).

### Survival

With regional survival, you should be able to observe forward progress for all 
accounts also during a full single region outage.

With zone survival, you should be able to observe forward progress for
all accounts homed in the other two operational regions. The accounts "homed" 
in the downed region will stop making progress until that region has recovered.

Making progress in this context means transfer and balance transactions
towards the accounts will succeed rather than timeout or fail.

### Consistency

With serializable isolation you will always observe consistent outcomes 
and zero balance sums per city (the checksum). With read committed isolation,
given that locking is enabled, you will also observe only correct transfer
fund outcomes.

### Locality

With table localities set to `regional-by-row`, you will observe predictable
low read and write latencies in the local regions. Replicas are however placed 
outside the row's home region for performance and survival. 

Complete data domiciling, where all voting and non-voting replicas are pinned 
to their respective home regions require the use of _super regions_ which is
out of scope for this tutorial.
