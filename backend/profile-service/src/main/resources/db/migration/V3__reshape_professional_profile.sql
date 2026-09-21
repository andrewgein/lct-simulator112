ALTER TABLE user_profiles
    DROP CONSTRAINT ck_user_profiles_training_specialization,
    DROP CONSTRAINT ck_user_profiles_emergency_service,
    DROP CONSTRAINT ck_user_profiles_professional_profile;

ALTER TABLE user_profiles
    RENAME COLUMN training_specialization TO training_track;

ALTER TABLE user_profiles
    RENAME COLUMN emergency_service TO dds_service;

UPDATE user_profiles
SET training_track = 'SYSTEM_112'
WHERE training_track = 'OPERATOR_112';

ALTER TABLE user_profiles
    ADD CONSTRAINT ck_user_profiles_training_track
        CHECK (training_track IS NULL OR training_track IN ('SYSTEM_112', 'DDS')),
    ADD CONSTRAINT ck_user_profiles_dds_service
        CHECK (dds_service IS NULL
            OR dds_service IN ('FIRE', 'POLICE', 'AMBULANCE', 'GAS', 'ANTI_TERROR')),
    ADD CONSTRAINT ck_user_profiles_professional_profile
        CHECK (
            (training_track IS NULL AND dds_service IS NULL)
            OR (training_track = 'SYSTEM_112' AND dds_service IS NULL)
            OR (training_track = 'DDS' AND dds_service IS NOT NULL)
        );
