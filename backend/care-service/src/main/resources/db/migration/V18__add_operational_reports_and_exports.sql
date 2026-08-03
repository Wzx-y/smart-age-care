create table care_export_job (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    export_type varchar(64) not null,
    period_start date not null,
    period_end date not null,
    status varchar(32) not null,
    file_name varchar(255),
    storage_key varchar(512),
    failure_message varchar(256),
    created_by bigint not null,
    created_at datetime(3) not null,
    completed_at datetime(3),
    expires_at datetime(3) not null,
    version bigint not null default 0,
    key idx_care_export_job_tenant_created (tenant_id, created_at),
    key idx_care_export_job_status_created (status, created_at),
    key idx_care_export_job_expiry (status, expires_at)
);

create table care_export_audit (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    export_job_id bigint not null,
    actor_id bigint,
    action varchar(32) not null,
    created_at datetime(3) not null,
    key idx_care_export_audit_tenant_job (tenant_id, export_job_id, created_at)
);
