alter table payments add column if not exists updated_at timestamp;
alter table payments add column if not exists processed_at timestamp;
