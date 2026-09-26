ALTER TABLE incidents DROP CONSTRAINT ck_incident_emergency_service;

UPDATE incidents
SET emergency_service = CASE emergency_service
    WHEN 'FIRE' THEN 'MCHS'
    WHEN 'GAS' THEN 'MOSGAZ'
    WHEN 'ANTI_TERROR' THEN 'FSB'
    ELSE emergency_service
END
WHERE emergency_service IN ('FIRE', 'GAS', 'ANTI_TERROR');
