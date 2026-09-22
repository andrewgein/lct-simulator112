ALTER TABLE incidents ADD COLUMN card_victim_count INTEGER NOT NULL DEFAULT 0;
UPDATE incidents
SET card_victim_count = 1
WHERE card_victim_first_name IS NOT NULL
   OR card_victim_last_name IS NOT NULL
   OR card_victim_phone IS NOT NULL;
ALTER TABLE incidents
    ADD CONSTRAINT ck_incident_card_victim_count CHECK (card_victim_count >= 0);
ALTER TABLE incidents DROP COLUMN card_victim_first_name;
ALTER TABLE incidents DROP COLUMN card_victim_last_name;
ALTER TABLE incidents DROP COLUMN card_victim_middle_name;
ALTER TABLE incidents DROP COLUMN card_victim_age;
ALTER TABLE incidents DROP COLUMN card_victim_phone;
ALTER TABLE incidents DROP COLUMN card_victim_contact_phone;
ALTER TABLE incidents DROP COLUMN card_victim_address;
ALTER TABLE incidents DROP COLUMN card_victim_additional_info;

ALTER TABLE system112_stage_details ADD COLUMN victim_count INTEGER NOT NULL DEFAULT 0;
UPDATE system112_stage_details
SET victim_count = 1
WHERE victim_first_name IS NOT NULL
   OR victim_last_name IS NOT NULL
   OR victim_phone IS NOT NULL;
ALTER TABLE system112_stage_details
    ADD CONSTRAINT ck_system112_stage_victim_count CHECK (victim_count >= 0);
ALTER TABLE system112_stage_details DROP COLUMN victim_first_name;
ALTER TABLE system112_stage_details DROP COLUMN victim_last_name;
ALTER TABLE system112_stage_details DROP COLUMN victim_middle_name;
ALTER TABLE system112_stage_details DROP COLUMN victim_age;
ALTER TABLE system112_stage_details DROP COLUMN victim_phone;
ALTER TABLE system112_stage_details DROP COLUMN victim_contact_phone;
ALTER TABLE system112_stage_details DROP COLUMN victim_address;
ALTER TABLE system112_stage_details DROP COLUMN victim_additional_info;
