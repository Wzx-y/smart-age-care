create table care_audit_log (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    actor_id bigint not null,
    action varchar(64) not null,
    target_type varchar(64) not null,
    target_id bigint not null,
    related_resident_id bigint,
    related_bed_id bigint,
    created_at datetime(3) not null,
    key idx_care_audit_tenant_target_created (tenant_id, target_type, target_id, created_at),
    key idx_care_audit_tenant_action_created (tenant_id, action, created_at)
);
