ALTER TABLE user_profiles DROP CONSTRAINT ck_user_profiles_dds_service;

UPDATE user_profiles
SET dds_service = CASE dds_service
    WHEN 'FIRE' THEN 'MCHS'
    WHEN 'GAS' THEN 'MOSGAZ'
    WHEN 'ANTI_TERROR' THEN 'FSB'
    ELSE dds_service
END
WHERE dds_service IN ('FIRE', 'GAS', 'ANTI_TERROR');
