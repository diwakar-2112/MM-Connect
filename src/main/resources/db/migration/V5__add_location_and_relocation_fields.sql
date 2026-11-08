-- Optional profile details; existing profiles remain readable without backfill.
ALTER TABLE profiles
    ADD COLUMN current_state VARCHAR(100),
    ADD COLUMN native_district VARCHAR(100),
    ADD COLUMN work_city VARCHAR(100),
    ADD COLUMN relocation_preference VARCHAR(30);
