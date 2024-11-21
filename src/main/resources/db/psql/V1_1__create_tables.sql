-- Leger DDL for PostgreSQL 10+

-- drop table if exists transfer_item cascade;
-- drop table if exists transfer cascade;
-- drop table if exists account cascade;
-- drop table if exists account_plan;
-- drop table if exists outbox cascade;

create extension if not exists pgcrypto;

drop function if exists gateway_region();

create function gateway_region() returns text
as
$$
select 'default'
$$
    language sql
    immutable
    returns null on null input;

drop type if exists account_type cascade;
create type account_type as enum ('Asset', 'Liability', 'Expense', 'Revenue', 'Equity');

drop type if exists transfer_type cascade;
create type transfer_type as enum ('Payment','Fee','Refund','Chargeback','Grant','Bank');

----------------------
-- Configuration
----------------------

create table account_plan
(
    name       varchar(32) not null,
    created_at timestamptz not null default clock_timestamp(),

    primary key (name)
);

----------------------
-- Main tables
----------------------

create table account
(
    id             uuid           not null default gen_random_uuid(),
    city           varchar(256)   not null,
    balance        decimal(19, 3) not null,
    currency       varchar(3)     not null default 'USD',
    name           varchar(128)   not null,
    description    varchar(512)   null,
    type           account_type   not null,
    closed         boolean        not null default false,
    allow_negative integer        not null default 0,
    updated_at     timestamptz    not null default clock_timestamp(),

    primary key (id)
);

create index account_city_storing_rec_idx on account (city)
    include (balance, currency, name, description, type, closed, allow_negative, updated_at);

create table transfer
(
    id            uuid          not null default gen_random_uuid(),
    city          varchar(256)  not null,
    booking_date  date          not null default CURRENT_DATE,
    transfer_date date          not null default CURRENT_DATE,
    transfer_type transfer_type not null,

    primary key (id)
);
create index on transfer (city);

create table transfer_item
(
    transfer_id     uuid           not null,
    account_id      uuid           not null,
    item_pos        int            not null,
    city            varchar(256)   not null,
    amount          decimal(19, 3) not null,
    currency        varchar(3)     not null default 'USD',
    note            varchar(512),
    running_balance decimal(19, 3) not null,

    primary key (transfer_id, account_id)
);

create index on transfer_item (city) include (amount, currency);

create table if not exists outbox
(
    id             uuid,
    aggregate_id   uuid,
    inserted_at    timestamptz not null default clock_timestamp(),
    aggregate_type varchar(32) not null,
    payload        jsonb       not null,

    primary key (id)
);

----------------------
-- Constraints
----------------------

alter table account
    add constraint check_account_allow_negative check (allow_negative between 0 and 1);
alter table account
    add constraint check_account_positive_balance check (balance * abs(allow_negative - 1) >= 0);

alter table transfer_item
    add constraint fk_txn_item_ref_transfer
        foreign key (transfer_id) references transfer (id);
alter table transfer_item
    add constraint fk_txn_item_ref_account
        foreign key (account_id) references account (id);