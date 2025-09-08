drop type if exists account_type;
create type account_type as enum ('Asset', 'Liability', 'Expense', 'Revenue', 'Equity');

drop type if exists transfer_type;
create type transfer_type as enum ('Payment','Fee','Refund','Chargeback','Grant','Bank');

----------------------
-- Configuration
----------------------

create table account_plan
(
    name       string(32) not null,
    created_at timestamptz not null default clock_timestamp(),

    primary key (name)
);

----------------------
-- Main tables
----------------------

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

create table transfer
(
    id            uuid          not null default gen_random_uuid(),
    city          string(128)   not null,
    booking_date  date          not null default current_date(),
    transfer_date date          not null default current_date(),
    transfer_type transfer_type not null,

    primary key (id)
);

create index on transfer (city);

create table transfer_item
(
    transfer_id           uuid           not null,
    account_id            uuid           not null,
    item_pos              int            not null,
    city                  string(128)    not null,
    amount                decimal(19, 3) not null,
    currency              string(3)      not null,
    amount_money          string as (concat(amount::string, ' ', currency)) virtual,
    note                  string,
    running_balance       decimal(19, 3) not null,
    running_balance_money string as (concat(running_balance::string, ' ', currency)) virtual,

    primary key (transfer_id, account_id, item_pos)
);

create index on transfer_item (city) storing (amount, currency);

create table if not exists outbox
(
    id             uuid as ((payload ->> 'eventId')::UUID) stored,
    aggregate_id   uuid as ((payload ->> 'entityId')::UUID) stored,
    inserted_at    timestamptz not null default clock_timestamp(),
    aggregate_type string(32) not null,
    payload        jsonb       not null,

    primary key (id)
);

alter table outbox set (ttl_expire_after = '1 hour');

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
