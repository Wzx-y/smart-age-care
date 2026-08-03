alter table care_admission
    add column reserved_until datetime(3) null after bed_id,
    add key idx_care_admission_reservation_expiry (status, reserved_until);
