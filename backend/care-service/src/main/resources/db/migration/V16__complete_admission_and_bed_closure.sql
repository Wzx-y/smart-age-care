create table care_admission_assessment (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    admission_id bigint not null,
    decision varchar(32) not null,
    conclusion varchar(500) not null,
    assessed_by bigint not null,
    assessed_at datetime(3) not null,
    key idx_care_admission_assessment_tenant_admission (tenant_id, admission_id, assessed_at)
);

create table care_bed_cleaning_record (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    bed_id bigint not null,
    result varchar(500) not null,
    cleaned_by bigint not null,
    cleaned_at datetime(3) not null,
    key idx_care_bed_cleaning_tenant_bed (tenant_id, bed_id, cleaned_at)
);

create table care_admission_bed_transfer (
    id bigint primary key auto_increment,
    tenant_id bigint not null,
    admission_id bigint not null,
    source_bed_id bigint not null,
    target_bed_id bigint not null,
    reason varchar(500) not null,
    transferred_by bigint not null,
    transferred_at datetime(3) not null,
    key idx_care_admission_transfer_tenant_admission (tenant_id, admission_id, transferred_at)
);
