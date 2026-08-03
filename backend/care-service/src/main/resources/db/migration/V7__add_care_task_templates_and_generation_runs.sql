create table care_plan_task_template (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    plan_id bigint not null,
    task_name varchar(128) not null,
    scheduled_time time not null,
    assignee_id bigint not null,
    active boolean not null default true,
    created_by bigint not null,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    unique key uk_care_plan_task_template_time (tenant_id, plan_id, task_name, scheduled_time),
    key idx_care_plan_task_template_tenant_plan_active (tenant_id, plan_id, active)
);

alter table care_task
    add column origin_template_id bigint,
    add column service_date date,
    add unique key uk_care_task_template_service_date (tenant_id, origin_template_id, service_date);

create table care_task_generation_run (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    service_date date not null,
    generated_task_count int not null,
    created_by bigint not null,
    created_at datetime(3) not null,
    key idx_care_task_generation_run_tenant_date (tenant_id, service_date)
);
