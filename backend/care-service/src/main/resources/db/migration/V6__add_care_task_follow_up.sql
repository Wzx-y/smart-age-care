create table care_task_follow_up (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    task_id bigint not null,
    resident_id bigint not null,
    status varchar(32) not null,
    exception_reason varchar(512) not null,
    resolution_note varchar(1024),
    created_by bigint not null,
    created_at datetime(3) not null,
    closed_by bigint,
    closed_at datetime(3),
    version bigint not null default 0,
    unique key uk_care_task_follow_up_task (task_id),
    key idx_care_task_follow_up_tenant_status_created (tenant_id, status, created_at),
    key idx_care_task_follow_up_tenant_resident (tenant_id, resident_id)
);
