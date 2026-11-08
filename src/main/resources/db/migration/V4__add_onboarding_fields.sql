-- Nullable columns preserve existing profiles. Required fields are enforced by
-- request validation so product rules can change without schema changes.
ALTER TABLE profiles
    ADD COLUMN mother_tongue VARCHAR(100),
    ADD COLUMN sub_community VARCHAR(100),
    ADD COLUMN paternal_gotra VARCHAR(100),
    ADD COLUMN maternal_gotra VARCHAR(100),
    ADD COLUMN sampradaya VARCHAR(100),
    ADD COLUMN mother_occupation VARCHAR(100),
    ADD COLUMN siblings VARCHAR(30),
    ADD COLUMN weekend_preference VARCHAR(100);

ALTER TABLE partner_preferences
    ADD COLUMN pref_marital_statuses TEXT[],
    ADD COLUMN exclude_paternal_gotra BOOLEAN,
    ADD COLUMN exclude_maternal_gotra BOOLEAN,
    ADD COLUMN pref_sub_communities TEXT[];
