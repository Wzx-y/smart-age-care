create table care_room (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    building varchar(64) not null,
    floor varchar(32) not null,
    room_no varchar(64) not null,
    room_type varchar(64) not null,
    created_at datetime(3) not null,
    unique key uk_care_room_tenant_no (tenant_id, room_no)
);

create table care_bed (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    room_id bigint not null,
    bed_no varchar(64) not null,
    occupancy_status varchar(32) not null default 'AVAILABLE',
    hygiene_status varchar(32) not null default 'READY',
    version bigint not null default 0,
    created_at datetime(3) not null,
    unique key uk_care_bed_room_no (room_id, bed_no),
    key idx_care_bed_tenant_status (tenant_id, occupancy_status)
);

create table care_resident (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    name varchar(128) not null,
    gender varchar(16),
    birth_date date,
    status varchar(32) not null,
    current_bed_id bigint,
    emergency_contact_name varchar(128) not null,
    emergency_contact_phone varchar(32),
    archived_at datetime(3),
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    key idx_care_resident_tenant_status (tenant_id, status),
    key idx_care_resident_tenant_name (tenant_id, name)
);

create table care_admission (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    resident_id bigint not null,
    bed_id bigint,
    status varchar(32) not null,
    applied_at datetime(3) not null,
    confirmed_at datetime(3),
    version bigint not null default 0,
    created_by bigint not null,
    updated_by bigint not null,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    key idx_care_admission_tenant_status (tenant_id, status),
    key idx_care_admission_resident (tenant_id, resident_id)
);
