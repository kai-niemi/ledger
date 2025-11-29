# Multi-region Tutorial

This page describes how to set up a complete multi-region demo or large scale load test.

## Create a CockroachDB cluster

This tutorial does not cover any CockroachDB cluster setups, see:
- https://www.cockroachlabs.com/docs/stable/manual-deployment
- https://www.cockroachlabs.com/docs/cockroachcloud

Assume you have a 9 node cluster with 8-32 vCPUs per node across 3 different regions, as follows.

| Region       | Primary | Secondary | Zones                                     | Nodes | Clients |
|--------------|:--------|:----------|-------------------------------------------|-------|:--------|
| us-east-1    |         |           | us-east-1a,us-east-1b,us-east-1c          | 1-3   | 10      |
| eu-central-1 | x       |           | eu-central-1a,eu-central-1b,eu-central-1c | 4-6   | 11      |
| eu-north-1   |         | x         | eu-north-1a,eu-north-1b,eu-north-1c       | 7-9   | 12      |

The _Nodes_ column refers to the CockroachDB node numbers. The _Clients_ column refers to the VMs/machines
hosting the ledger server, a CockroachDB binary and HAProxy load balancer in case it's a self-hosted cluster.

## Deploy the clients

Assume each client (10-12) is already prepared with the required dependencies:

- CockroachDB binary
- Ledger JAR file
- JDK21

The rest of this tutorial assumes a self-hosted, secure CockroachDB cluster.

### Client 10

SSH to the client and create the database:
                      
    ssh <client-host-10>
    ./cockroach sql --certs-dir=certs --host=<ip> -e "CREATE DATABASE ledger; ALTER ROLE root WITH PASSWORD 'cockroach'"

Generate a config file for `us-east-1` region and start haproxy:

    ./cockroach gen haproxy --certs-dir=certs --host <ip> --external --locality=region=us-east-1
    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

### Client 11

Generate a config file for `eu-central-1` region and start haproxy:

    ssh <client-host-11>
    ./cockroach gen haproxy --certs-dir=certs --host <ip> --external --locality=region=eu-central-1
    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

### Client 12

Generate a config file for `eu-north-1` region and start haproxy:
                        
    ssh <client-host-12>
    ./cockroach gen haproxy --certs-dir=certs --host <ip> --external --locality=region=eu-north-1
    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

## Start the servers

In a multi-region setup like this, you should enable the multi-region capabilities including:

- Primary and secondary regions
- Table localities (global and regional-by-row)
- (optional) Survival goal from zone to region

There is a single shell command `apply-multi-region` that will enable all of the above.

### Client 10

Start the server and build the account plan:

    ssh <client-host-10>
    java -jar ledger.jar
    :$ build-account-plan

Enable multi-region features in one go:

    :$ apply-multi-region

This will add database regions, set the survival goal to `REGION` and also set
table localities to `regional-by-row`. It will make the home region of a table
row to be determined by the value of its city column by using the `region` computed 
column as partition key. 

Notice that the DDL operations can be quite heavyso it may take a while to run and 
for the database to re-balance the data accordingly to the placement constraints. 
You can observe this activity in the DB Console.

After things have settled, start a few workloads for the first region, for example:

    :$ transfer-funds 
    :$ read-balance

You should notice that it picks the cities for the local region, in this case `us-east-1`.
As a later exercise, you can change the survival goal back to `zone` (the default) 
and observe its effect.

### Client 11

Start the server shell and some workloads:
                   
    ssh <client-host-11>
    java -jar ledger.jar 
    :$ transfer-funds 
    :$ read-balance

You should notice that it picks the cities of the local region (eu-central-1).

### Client 12

Start the server shell and some workloads:
                        
    ssh <client-host-12>
    java -jar ledger.jar 
    :$ transfer-funds 
    :$ read-balance

You should notice that it picks the cities of the local region (eu-north-1).

## Takeaways

Key takeaways in running ledger in a multi-region setup.
                                         
### Performance

Granted that you are using zone survival, you should observe local read and write 
latencies for all transactions towards "in-region" accounts since the accounts 
have been optimized for access from local regions. If you are using region survival,
then the write latency is affected by at most one cross-region link latency. 
You should also observe higher read and write latencies when transferring 
funds across remote regions.

A 3x16 vCPU single-region cluster with a 2GB dataset should provide approximately:

- 60K QPS at P99 <1ms for point balance reads.
- 40K QPS at P99 20ms P99 for read-write transactions (transfers).

A 9x16 vCPU multi-region cluster should provide similar latencies and a higher
QPS number.

### Survival

With zone survival you will observe forward progress for all accounts homed 
in each region in the event of 1 of 3 zones being out. With regional survival, 
you will observe forward progress for all accounts across the entire keyspace 
in the event of 1 of 3 regions being out.

Forward progress in this context means account transfers and balance requests
will succeed rather than timeout or error out.

### Consistency

With serializable isolation you will always observe consistent transfer 
outcomes and zero balance sums per city (called checksums). 

With read committed isolation, given that locking is enabled, you will 
also observe correct transfer fund outcomes. Without locking, you may 
observe P4 lost update anomalies with negative balances as an outcome.

### Locality

Tables with locality set to `regional-by-row` will observe predictable
low read and write latencies in the local regions. Non-voting replicas 
are still placed outside the row's home region for performance and survival,
hence there's no data domiciling enabled. Data domiciling, where all voting 
and non-voting replicas are pinned to their respective home regions, would 
require the use of _super regions_ which is out of scope for this tutorial.
