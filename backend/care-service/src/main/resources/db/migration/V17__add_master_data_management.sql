alter table care_room
    add column nursing_unit varchar(64) not null default '' after room_type,
    add column enabled char(1) not null default '0' after nursing_unit,
    add column version bigint not null default 0 after enabled,
    add column updated_at datetime(3) not null default current_timestamp(3) after created_at;

alter table care_bed
    add column equipment_summary varchar(500) not null default '' after bed_no,
    add column enabled char(1) not null default '0' after hygiene_status,
    add column updated_at datetime(3) not null default current_timestamp(3) after created_at;

create table care_master_data_item (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    category varchar(32) not null,
    item_code varchar(64) not null,
    item_name varchar(128) not null,
    description varchar(500) not null default '',
    sort_order int not null default 0,
    enabled char(1) not null default '0',
    version bigint not null default 0,
    created_by bigint not null,
    updated_by bigint not null,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    unique key uk_care_master_data_tenant_category_code (tenant_id, category, item_code),
    key idx_care_master_data_tenant_category_enabled (tenant_id, category, enabled, sort_order)
);

create table care_master_data_audit (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    actor_id bigint not null,
    resource_type varchar(32) not null,
    resource_id bigint not null,
    action varchar(32) not null,
    created_at datetime(3) not null,
    key idx_care_master_data_audit_tenant_resource (tenant_id, resource_type, resource_id, created_at)
);
