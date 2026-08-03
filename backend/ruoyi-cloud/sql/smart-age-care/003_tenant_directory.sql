create table if not exists care_tenant_directory (
    tenant_id bigint not null auto_increment,
    tenant_code varchar(64) not null,
    tenant_name varchar(128) not null,
    plan_code varchar(32) not null default 'STANDARD',
    region varchar(128) not null default '',
    status char(1) not null default '0',
    version bigint not null default 0,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    primary key (tenant_id),
    unique key uk_care_tenant_directory_code (tenant_code),
    key idx_care_tenant_directory_status (status)
) engine=InnoDB default charset=utf8mb4;
