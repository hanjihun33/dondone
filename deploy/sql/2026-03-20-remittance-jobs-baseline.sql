-- Remittance + Jobs schema baseline
-- Manual reference SQL for the schema-first refactor.
-- This file is not auto-applied by Spring Boot.

create table if not exists user_wallets (
    user_id bigint primary key,
    wallet_address varchar(42) not null unique,
    encrypted_private_key varchar(1024) not null,
    funding_status varchar(20) not null,
    funding_failure_reason varchar(300),
    funded_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_user_wallets_user foreign key (user_id) references users(id)
);

create index if not exists idx_user_wallets_funding_status_updated_at
    on user_wallets (funding_status, updated_at desc, user_id desc);

create table if not exists recipients (
    recipient_id varchar(64) primary key,
    user_id bigint not null,
    target_user_id bigint,
    alias varchar(100) not null,
    relation varchar(20) not null,
    wallet_address varchar(42) not null,
    allowed boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_recipients_user foreign key (user_id) references users(id),
    constraint fk_recipients_target_user foreign key (target_user_id) references users(id),
    constraint uk_recipients_user_wallet unique (user_id, wallet_address)
);

create index if not exists idx_recipients_user_updated_at
    on recipients (user_id, updated_at desc, recipient_id desc);

create index if not exists idx_recipients_target_user_id
    on recipients (target_user_id);

create table if not exists transfers (
    transfer_id varchar(64) primary key,
    user_id bigint not null,
    recipient_id varchar(64) not null,
    asset_symbol varchar(20) not null,
    amount_atomic bigint not null,
    sender_address varchar(42) not null,
    recipient_address varchar(42) not null,
    recipient_alias_snapshot varchar(100) not null,
    recipient_relation_snapshot varchar(20) not null,
    recipient_target_user_id_snapshot bigint,
    status varchar(20) not null,
    idempotency_key varchar(128) not null,
    high_amount_confirmed boolean not null,
    recent_recipient_confirmed boolean not null,
    tx_hash varchar(66),
    signed_transaction text,
    failure_code varchar(40),
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_transfers_user foreign key (user_id) references users(id),
    constraint fk_transfers_recipient foreign key (recipient_id) references recipients(recipient_id),
    constraint uk_transfers_user_idempotency unique (user_id, idempotency_key),
    constraint uk_transfers_tx_hash unique (tx_hash)
);

create index if not exists idx_transfers_user_created_at
    on transfers (user_id, created_at desc, transfer_id desc);

create index if not exists idx_transfers_status_updated_at
    on transfers (status, updated_at desc, transfer_id desc);

create index if not exists idx_transfers_user_status
    on transfers (user_id, status);

create table if not exists jobs (
    id bigserial primary key,
    job_type varchar(40) not null,
    reference_kind varchar(40) not null,
    reference_id varchar(64) not null,
    active_key varchar(200),
    status varchar(20) not null,
    attempt_count integer not null,
    run_at timestamp not null,
    last_error varchar(500),
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_jobs_active_key unique (active_key)
);

create index if not exists idx_jobs_status_run_at
    on jobs (status, run_at, id);

create index if not exists idx_jobs_reference_kind_id_type
    on jobs (reference_kind, reference_id, job_type);
