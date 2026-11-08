INSERT INTO match_profiles (
    user_id,
    gender,
    birth_date,
    age,
    marital_status,
    religion,
    community,
    current_city,
    pref_age_min,
    pref_age_max,
    pref_locations,
    is_active,
    is_verified,
    updated_at
)
SELECT
    p.user_id,
    p.gender,
    p.date_of_birth,
    (EXTRACT(YEAR FROM CURRENT_DATE) - EXTRACT(YEAR FROM p.date_of_birth))::INT AS age,
    p.marital_status,
    p.religion,
    p.community,
    p.current_city,
    pp.pref_age_min,
    pp.pref_age_max,
    pp.pref_locations,
    true,   -- default is_active
    false,  -- default is_verified
    CURRENT_TIMESTAMP
FROM profiles p
JOIN partner_preferences pp ON p.user_id = pp.user_id
WHERE NOT EXISTS (
    -- Only copy them if they are NOT ALREADY in the match_profiles table!
    SELECT 1 FROM match_profiles m WHERE m.user_id = p.user_id
);