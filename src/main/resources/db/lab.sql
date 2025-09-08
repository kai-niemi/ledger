-- set enable_multiregion_placement_policy = on;
-- alter database ledger placement restricted;
-- alter database ledger placement default;

-- set enable_super_regions = 'on';
-- alter role all set enable_super_regions = on;
-- alter database ledger add super region "eu" values "eu-north-1","eu-central-1","eu-west-1";
-- alter database ledger add super region "us" values "us-east-1","us-east-2","us-west-1";
-- alter database ledger survive region failure;
-- show super regions from database ledger;

--
-- Add regions
--

show regions;
alter database ledger primary region "eu-central-1";
alter database ledger add region "eu-north-1";
alter database ledger add region "eu-west-1";

--
-- Create table
--

drop type if exists account_type;
create type account_type as enum ('Asset', 'Liability', 'Expense', 'Revenue', 'Equity');

create table account
(
    id             uuid           not null default gen_random_uuid(),
    city           string(128)        not null,
    balance        decimal(19, 3) not null,
    currency       string (3)         not null,
    balance_money  string as (concat(balance::string, ' ', currency)) virtual,
    name           string(128) not null,
    description    string(256) null,
    type           account_type   not null,
    closed         boolean        not null default false,
    allow_negative integer        not null default 0,
    updated_at     timestamptz    not null default clock_timestamp(),

    primary key (id)
);

create index if not exists account_city_storing_rec_idx on account (city)
    storing (balance, currency, name, description, type, closed, allow_negative, updated_at);

--
-- Add RBR computed column
--

ALTER TABLE account
    ADD COLUMN region crdb_internal_region AS (
        CASE
            WHEN city IN ('stockholm', 'copenhagen', 'helsinki', 'oslo', 'riga', 'tallinn') THEN 'eu-north-1'
            WHEN city IN ('dublin', 'belfast', 'london', 'liverpool', 'manchester', 'glasgow', 'birmingham', 'leeds') THEN 'eu-west-1'
            ELSE 'eu-central-1'
            END
        ) STORED NOT NULL;

show create table account;
alter table account set locality regional by row as region;

--
-- Add test data
--

insert into account (city, balance, currency, name, type, closed, allow_negative)
select
    'stockholm',
    '100.00',
    'SEK',
    (concat('user:', no::text)),
    'Asset',
    false,
    0
from generate_series(1, 10) no;

insert into account (city, balance, currency, name, type, closed, allow_negative)
select
    'dublin',
    '100.00',
    'EUR',
    (concat('user:', no::text)),
    'Asset',
    false,
    0
from generate_series(1, 10) no;

insert into account (city, balance, currency, name, type, closed, allow_negative)
select
    'berlin',
    '100.00',
    'EUR',
    (concat('user:', no::text)),
    'Asset',
    false,
    0
from generate_series(1, 10) no;

-- explain analyze
select region,id,balance from account where city = 'stockholm';
select region,id,balance from account where city = 'berlin';
select region,id,balance from account where city = 'dublin';

--
-- Sandbox
--

show ranges from table account;
show ranges from index account@primary;
show range from table account for row ('eu-north-1', '02fde064-10b1-4568-be1c-9012f97cd448');
show range from table account for row ('eu-west-1', '02fde064-10b1-4568-be1c-9012f97cd448');
show range from table account for row ('eu-central-1', '02fde064-10b1-4568-be1c-9012f97cd448');

select survival_goal from [show databases] where database_name='ledger';
alter database ledger survive region failure;
alter database ledger survive zone failure;

set enable_multiregion_placement_policy = on;
alter database ledger placement restricted;
alter database ledger placement default;
