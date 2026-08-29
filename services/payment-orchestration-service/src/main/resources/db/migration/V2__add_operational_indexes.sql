create index if not exists idx_payments_status
    on payments(status);

create index if not exists idx_payments_created_at
    on payments(created_at);

create index if not exists idx_payments_status_created_at
    on payments(status, created_at);

create index if not exists idx_outbox_events_status_created_at
    on outbox_events(status, created_at);

create index if not exists idx_outbox_events_aggregate_id
    on outbox_events(aggregate_id);
