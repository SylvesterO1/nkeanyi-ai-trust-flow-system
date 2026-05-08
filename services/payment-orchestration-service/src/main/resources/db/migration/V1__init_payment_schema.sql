create table if not exists payments (
    id bigserial primary key,
    payment_id varchar(64) not null,
    idempotency_key varchar(100) not null,
    customer_id varchar(64) not null,
    source_account varchar(64) not null,
    destination_account varchar(64) not null,
    amount numeric(19,2) not null,
    currency varchar(10) not null,
    payment_reference varchar(128),
    payment_method varchar(50),
    narration varchar(255),
    status varchar(30) not null,
    risk_score double precision,
    decision_reason varchar(255),
    created_at timestamp not null
);

create unique index if not exists uk_payments_payment_id
    on payments(payment_id);

create unique index if not exists uk_payments_idempotency_key
    on payments(idempotency_key);

create table if not exists outbox_events (
    id bigserial primary key,
    event_id varchar(64) not null,
    aggregate_type varchar(50) not null,
    aggregate_id varchar(64) not null,
    topic varchar(100) not null,
    payload text not null,
    status varchar(20) not null,
    created_at timestamp not null,
    published_at timestamp
);

create unique index if not exists uk_outbox_events_event_id
    on outbox_events(event_id);
