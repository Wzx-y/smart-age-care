create table care_write_idempotency (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    actor_id bigint not null,
    operation varchar(64) not null,
    idempotency_key varchar(128) not null,
    request_fingerprint char(64) not null,
    status varchar(32) not null,
    result_resource_type varchar(32),
    result_resource_id bigint,
    result_status varchar(32),
    result_version bigint,
    result_detail varchar(1024),
    created_at datetime(3) not null,
    completed_at datetime(3),
    unique key uk_care_write_idempotency_scope (tenant_id, actor_id, operation, idempotency_key),
    key idx_care_write_idempotency_created (tenant_id, created_at)
);
