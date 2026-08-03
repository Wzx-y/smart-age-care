create table care_notification (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    recipient_id bigint not null,
    category varchar(32) not null,
    priority varchar(16) not null,
    title varchar(160) not null,
    resource_type varchar(64) not null,
    resource_id bigint not null,
    related_resident_id bigint,
    dedupe_key varchar(128) not null,
    created_at datetime(3) not null,
    read_at datetime(3),
    unique key uk_care_notification_recipient_dedupe (tenant_id, recipient_id, dedupe_key),
    key idx_care_notification_recipient_read_created (tenant_id, recipient_id, read_at, created_at)
);

create table care_notification_read (
    tenant_id bigint not null,
    notification_id bigint not null,
    recipient_id bigint not null,
    read_at datetime(3) not null,
    primary key (tenant_id, notification_id, recipient_id),
    key idx_care_notification_read_recipient (tenant_id, recipient_id, read_at)
);

alter table care_audit_log add key idx_care_audit_tenant_created (tenant_id, created_at);
