create table if not exists payment_idempotency_keys (
    id bigserial primary key,
    idempotency_key varchar(100) not null,
    customer_id varchar(64) not null,
    request_hash varchar(128) not null,
    payment_db_id bigint not null,
    payment_id varchar(64) not null,
    created_at timestamp not null,
    last_replayed_at timestamp,
    replay_count bigint not null default 0
);

create unique index if not exists uk_payment_idempotency_keys_key
    on payment_idempotency_keys(idempotency_key);

create index if not exists idx_payment_idempotency_customer
    on payment_idempotency_keys(customer_id);

create index if not exists idx_payment_idempotency_payment_id
    on payment_idempotency_keys(payment_id);

create table if not exists payment_status_events (
    id bigserial primary key,
    payment_db_id bigint not null,
    payment_id varchar(64) not null,
    from_status varchar(30),
    to_status varchar(30) not null,
    reason varchar(255),
    actor varchar(100),
    event_time timestamp not null
);

create index if not exists idx_payment_status_events_payment_id
    on payment_status_events(payment_id);

create index if not exists idx_payment_status_events_event_time
    on payment_status_events(event_time);

create table if not exists payment_risk_decisions (
    id bigserial primary key,
    payment_db_id bigint not null,
    payment_id varchar(64) not null,
    risk_score double precision not null,
    decision varchar(40) not null,
    reason varchar(255),
    rules_triggered text,
    created_at timestamp not null
);

create index if not exists idx_payment_risk_decisions_payment_id
    on payment_risk_decisions(payment_id);

create table if not exists ledger_entries (
    id bigserial primary key,
    payment_db_id bigint not null,
    payment_id varchar(64) not null,
    entry_type varchar(40) not null,
    account_ref varchar(64) not null,
    amount numeric(19,2) not null,
    currency varchar(10) not null,
    direction varchar(20) not null,
    status varchar(30) not null,
    created_at timestamp not null
);

create index if not exists idx_ledger_entries_payment_id
    on ledger_entries(payment_id);

create index if not exists idx_ledger_entries_account_ref
    on ledger_entries(account_ref);
