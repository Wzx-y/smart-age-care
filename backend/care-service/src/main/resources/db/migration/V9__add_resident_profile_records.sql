create table care_resident_contact (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    resident_id bigint not null,
    name varchar(128) not null,
    relationship_text varchar(64) not null,
    phone varchar(32),
    is_primary boolean not null default false,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    key idx_care_resident_contact_tenant_resident (tenant_id, resident_id)
);

create table care_resident_assessment (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    resident_id bigint not null,
    assessment_type varchar(64) not null,
    assessment_date date not null,
    score integer,
    risk_level varchar(32) not null,
    note varchar(1024),
    assessor_id bigint not null,
    created_at datetime(3) not null,
    key idx_care_resident_assessment_tenant_resident_date (tenant_id, resident_id, assessment_date desc)
);

create table care_resident_attachment (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    resident_id bigint not null,
    file_name varchar(255) not null,
    content_type varchar(128) not null,
    byte_size bigint not null,
    storage_key varchar(512) not null,
    upload_status varchar(32) not null,
    created_by bigint not null,
    created_at datetime(3) not null,
    unique key uk_care_resident_attachment_storage_key (storage_key),
    key idx_care_resident_attachment_tenant_resident (tenant_id, resident_id)
);
