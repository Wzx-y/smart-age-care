create table care_shift_handover (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    shift_date date not null,
    shift_code varchar(32) not null,
    from_user_id bigint not null,
    to_user_id bigint not null,
    note varchar(1024) not null,
    status varchar(32) not null,
    submitted_at datetime(3),
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    version bigint not null default 0,
    unique key uk_care_shift_handover_sender_shift (tenant_id, shift_date, shift_code, from_user_id),
    key idx_care_shift_handover_tenant_status_date (tenant_id, status, shift_date)
);

create table care_shift_handover_item (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    handover_id bigint not null,
    task_id bigint not null,
    created_at datetime(3) not null,
    unique key uk_care_shift_handover_item (handover_id, task_id),
    key idx_care_shift_handover_item_tenant_task (tenant_id, task_id)
);
