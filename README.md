[![Java CI](https://github.com/kai-niemi/ledger/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/kai-niemi/ledger/actions/workflows/maven.yml)

<!-- TOC -->
* [Ledger](#ledger)
  * [Compatibility](#compatibility)
* [Terms of Use](#terms-of-use)
* [Setup](#setup)
  * [Prerequisites](#prerequisites)
  * [Install the JDK](#install-the-jdk)
  * [Database Setup](#database-setup)
    * [CockroachDB Setup](#cockroachdb-setup)
    * [PostgreSQL Setup (optional)](#postgresql-setup-optional)
  * [Building](#building)
    * [Clone the project](#clone-the-project)
    * [Build the artifacts](#build-the-artifacts)
* [Configuration](#configuration)
* [Running](#running)
  * [Run with an interactive shell](#run-with-an-interactive-shell)
  * [Start in the background](#start-in-the-background)
  * [Enable PostgreSQL](#enable-postgresql)
* [Single-region Tutorial](#single-region-tutorial)
  * [Basics](#basics)
  * [Create an account plan](#create-an-account-plan)
  * [Transfer funds](#transfer-funds)
  * [Read balance](#read-balance)
  * [Create accounts](#create-accounts)
  * [Transfer grants](#transfer-grants)
* [Multi-region Tutorial](#multi-region-tutorial)
<!-- TOC -->

# Ledger

<img align="left" src=".github/logo.png" alt="" width="64"/> 

Ledger represents a full-stack, financial accounting ledger based on the double-entry
bookkeeping principle, running on either [CockroachDB](https://www.cockroachlabs.com/) or PostgreSQL. It manages monetary 
accounts and a journal of balanced, multi-legged, multi-currency transfers between those accounts. 

It's designed to showcase CockroachDB's scalability, survival, consistency and data domiciling 
capabilities and not the domain complexity of accounting. However, it's a realistic implementation 
of a general ledger with most fundamental pieces.

Ledger is the successor to [Roach Bank](https://github.com/kai-niemi/roach-bank) with more focus on 
load testing and operational ease-of-use. Conceptually they are the same, but Ledger has a simpler 
design, improved UI and leverages JDK21 features such as virtual threads for better efficiency.

## Compatibility

- CockroachDB v23.2+
- PostgreSQL 9+
- MacOS (main platform)
- Linux
- JDK 21

# Terms of Use

This tool is not supported by Cockroach Labs. Use of this tool is entirely at your
own risk and Cockroach Labs makes no guarantees or warranties about its operation.

See [MIT](LICENSE.txt) for terms and conditions.

# Setup

Things you need to build and run Ledger locally.

## Prerequisites

- Java 21+ JDK
    - https://openjdk.org/projects/jdk/21/
    - https://www.oracle.com/java/technologies/downloads/#java21
- CockroachDB Cloud or self-hosted 23.2+
- PostgreSQL 9+ (optional)

## Install the JDK

MacOS (using sdkman):

    curl -s "https://get.sdkman.io" | bash
    sdk list java
    sdk install java 21.0 (pick version)  

Ubuntu:

    sudo apt-get install openjdk-21-jdk

## Database Setup

### CockroachDB Setup

See [start a local cluster](https://www.cockroachlabs.com/docs/v24.2/start-a-local-cluster) for setup instructions.

Then create the database, in this case assuming an insecure cluster:

    cockroach sql --insecure -e "create database ledger"

An [enterprise license](https://www.cockroachlabs.com/docs/stable/licensing-faqs.html#obtain-a-license)
is only required when using geo-partitioning and follower-reads (opt-out features if n/a).

To set an enterprise license:

    cockroach sql --insecure --host=localhost -e "SET CLUSTER SETTING cluster.organization = '...'; SET CLUSTER SETTING enterprise.license = '...';"

### PostgreSQL Setup (optional)

Install on MacOS using brew:

    brew install postgresql

Start PSQL:

    brew services start postgresql@14

Create the database:

    psql postgres
    $ CREATE database ledger;
    $ CREATE extension pgcrypto;
    $ quit

Stop PSQL:

    brew services stop postgresql@14

## Building

Instructions for building the project locally.

### Clone the project

    git clone git@github.com:kai-niemi/ledger.git && cd ledger

### Build the artifacts

    chmod +x mvnw
    ./mvnw clean install

# Configuration

All configuration properties can be specified in [config/application-default.yml](config/application-default.yml) that 
overrides the baseline configuration in [src/main/resources/application.yml](src/main/resources/application.yml) 
(see this file for a reference on all options available).

You can also override all parameters through the command line, which is the easiest approach: 

    java -jar ledger.jar \
    --spring.datasource.url="jdbc:postgresql://localhost:26257/ledger??ssl=true&sslmode=require" \
    --spring.datasource.username=craig \
    --spring.datasource.password=cockroach \
    --spring.profiles.active="default,local"

Alternatively, you can create a new YAML file with a custom name suffix and then pass that
name with the `--profiles` argument:

    cp src/main/resources/application.yml config/application-craig.yml
    java -jar ledger.jar --spring.profiles.active="craig"

# Running

## Run with an interactive shell

Start the server with:

    java -jar target/ledger-<version>.jar <args>

(or use the `./run-server.sh` file).

Now you can access the application via http://localhost:9090.

## Start in the background
              
Add the shell commands you would like to run into a plain text file:

    echo "help" > cmd.txt
    echo "version" >> cmd.txt
    echo "transfer-funds" >> cmd.txt
    echo "read-balance" >> cmd.txt

Start the server in the background by passing the command file as an argument:

    nohup java -jar target/ledger-<version>.jar @cmd.txt > ledger-stdout.log 2>&1 &

(or use the `./start-server.sh` file)

The server will run all commands in the text file and then wait to be closed. 
Notice that you can't use `quit` in the end since all commands are run in parallel.

## Enable PostgreSQL

PostgreSQL is enabled by activating the `psql` profile:

    java -jar ledger.jar \
    --spring.datasource.url="jdbc:postgresql://localhost:5432/ledger \
    --spring.datasource.username=craig \
    --spring.datasource.password=cockroach \
    --spring.profiles.active="default,local,psql"

# Single-region Tutorial

Usage tutorial for running a basic demo or load test towards a single-region or single-host CockroachDB cluster.

## Basics

Ledger is operated entirely through its build-in shell, or by a command file passed at startup
time, in which case the shell is disabled (see [running](#running) instructions above).

There are also many commands not listed here. For a complete list run:

    help

Ledger also provides a reactive web UI available at http://localhost:9090 (default port).

## Create an account plan
    
The first step is to create an account plan:

    build-account-plan

This command will create one _liability_ account and 5,000 _asset_ accounts per city by default. The account plan
is organized in such a way that the total balance of all accounts for a given city (and currency) amounts to zero. Thus,
if a non-zero total is ever observed, it means money has been invented or destroyed and we can't have that.

The account plan can be dropped and recreated, which is a destructive operation (truncating the app tables).
      
## Transfer funds

Now that there's an account plan, it unlocks most of the different workloads available. One read-write workload
is to transfer funds between asset accounts in a random and rapid fashion. This is done through balanced, multi-legged
transfers that executes in parallel.

    transfer-funds
                  
By default, all workloads select the cities in the gateway region. You can however pick any region or all of them,
which we will find useful in the multi-region scenario (below). 

To see all options run:

    help transfer-funds

## Read balance

This workload is pure read-only and executes point lookups on accounts to retrieve the current balance.

    read-balance

One variant is to use historical follower reads, which, if you are familiar with CockroachDB, means that 
any node receiving a request hosting a range replica for the requested key can service the request, 
at the expense of the returned value being potentially stale (up to ~5s).

    read-balance-historical

## Create accounts

This command create new asset accounts in batches. Notice that these accounts have a zero balance
and are not allowed to go negative. To fund these accounts, you need to run the next transfer command
to grant funds from liability accounts.

    create-accounts

## Transfer grants

Similar to the transfer funds command, this one will move funds from liability accounts to asset accounts
with a specified balance range. It's useful to run this after creating new accounts to allow these
accounts to become part of the selection.

    transfer-grants
  
# Multi-region Tutorial

For the complete multi-region guide, see [tutorial](TUTORIAL.md).

---

-- The end