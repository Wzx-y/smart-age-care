create table if not exists care_tenant_member (
    tenant_id bigint not null,
    user_id bigint not null,
    status char(1) not null default '0',
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    primary key (tenant_id, user_id),
    key idx_care_tenant_member_user_status (user_id, status)
);

create table if not exists care_tenant_member_role (
    tenant_id bigint not null,
    user_id bigint not null,
    role_id bigint not null,
    created_at datetime not null default current_timestamp,
    primary key (tenant_id, user_id, role_id),
    key idx_care_tenant_member_role_role (role_id)
);

-- Run after RuoYi's system SQL. A member's business roles are assigned inside a tenant,
-- while role definitions and menu permissions remain RuoYi's sys_role/sys_role_menu records.
