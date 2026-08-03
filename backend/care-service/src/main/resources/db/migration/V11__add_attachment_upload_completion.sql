alter table care_resident_attachment
    add column uploaded_at datetime(3) null after created_at;
