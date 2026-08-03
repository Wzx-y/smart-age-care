alter table care_resident_contact
    add column version bigint not null default 1 after is_primary;

alter table care_resident_assessment
    add column version bigint not null default 1 after assessor_id;

alter table care_plan
    add column assessment_id bigint null after resident_id,
    add key idx_care_plan_tenant_resident_assessment (tenant_id, resident_id, assessment_id);
