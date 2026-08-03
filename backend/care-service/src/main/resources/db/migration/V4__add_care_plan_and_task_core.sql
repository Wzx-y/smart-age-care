create table care_plan (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    resident_id bigint not null,
    plan_name varchar(128) not null,
    frequency_text varchar(128) not null,
    owner_id bigint not null,
    status varchar(32) not null,
    version bigint not null,
    start_date date not null,
    end_date date,
    published_at datetime(3),
    created_by bigint not null,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    key idx_care_plan_tenant_resident_status (tenant_id, resident_id, status),
    unique key uk_care_plan_tenant_resident_version (tenant_id, resident_id, version)
);

create table care_task (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    plan_id bigint not null,
    resident_id bigint not null,
    task_name varchar(128) not null,
    scheduled_at datetime(3) not null,
    assignee_id bigint not null,
    status varchar(32) not null,
    exception_reason varchar(512),
    completed_at datetime(3),
    completed_by bigint,
    version bigint not null default 0,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    key idx_care_task_tenant_schedule_status (tenant_id, scheduled_at, status),
    key idx_care_task_tenant_resident (tenant_id, resident_id)
);

create table care_service_record (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    task_id bigint not null,
    executor_id bigint not null,
    result_note varchar(1024) not null,
    completed_at datetime(3) not null,
    created_at datetime(3) not null,
    unique key uk_care_service_record_task (task_id),
    key idx_care_service_record_tenant_completed (tenant_id, completed_at)
);
