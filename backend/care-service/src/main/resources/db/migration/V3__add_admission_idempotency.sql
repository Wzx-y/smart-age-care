create table care_admission_idempotency (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    actor_id bigint not null,
    operation varchar(64) not null,
    idempotency_key varchar(128) not null,
    request_fingerprint char(64) not null,
    status varchar(32) not null,
    result_admission_id bigint,
    result_resident_id bigint,
    result_bed_id bigint,
    result_admission_status varchar(32),
    result_admission_version bigint,
    result_applied_at datetime(3),
    created_at datetime(3) not null,
    completed_at datetime(3),
    unique key uk_care_admission_idempotency_scope (tenant_id, actor_id, operation, idempotency_key),
    key idx_care_admission_idempotency_created (tenant_id, created_at)
);
