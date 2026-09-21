ALTER TABLE user_profiles
    ADD COLUMN training_specialization VARCHAR(50),
    ADD COLUMN emergency_service VARCHAR(50),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE user_profiles
    ADD CONSTRAINT ck_user_profiles_training_specialization
        CHECK (training_specialization IS NULL
            OR training_specialization IN ('OPERATOR_112', 'DDS')),
    ADD CONSTRAINT ck_user_profiles_emergency_service
        CHECK (emergency_service IS NULL
            OR emergency_service IN ('FIRE', 'POLICE', 'AMBULANCE', 'GAS', 'ANTI_TERROR')),
    ADD CONSTRAINT ck_user_profiles_professional_profile
        CHECK (
            (training_specialization IS NULL AND emergency_service IS NULL)
            OR (training_specialization = 'OPERATOR_112' AND emergency_service IS NULL)
            OR (training_specialization = 'DDS' AND emergency_service IS NOT NULL)
        );
