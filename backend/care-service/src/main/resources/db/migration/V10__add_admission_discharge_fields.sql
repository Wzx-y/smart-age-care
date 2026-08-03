alter table care_admission
    add column discharged_at datetime(3) null after confirmed_at,
    add column discharged_by bigint null after discharged_at,
    add column discharge_reason varchar(500) null after discharged_by;
