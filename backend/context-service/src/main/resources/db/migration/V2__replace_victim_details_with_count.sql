ALTER TABLE incident_contexts ADD COLUMN card_victim_count INTEGER NOT NULL DEFAULT 0;
UPDATE incident_contexts
SET card_victim_count = 1
WHERE card_victim_first_name IS NOT NULL
   OR card_victim_last_name IS NOT NULL
   OR card_victim_phone IS NOT NULL;
ALTER TABLE incident_contexts
    ADD CONSTRAINT ck_context_card_victim_count CHECK (card_victim_count >= 0);
ALTER TABLE incident_contexts DROP COLUMN card_victim_first_name;
ALTER TABLE incident_contexts DROP COLUMN card_victim_last_name;
ALTER TABLE incident_contexts DROP COLUMN card_victim_middle_name;
ALTER TABLE incident_contexts DROP COLUMN card_victim_age;
ALTER TABLE incident_contexts DROP COLUMN card_victim_phone;
ALTER TABLE incident_contexts DROP COLUMN card_victim_contact_phone;
ALTER TABLE incident_contexts DROP COLUMN card_victim_address;
ALTER TABLE incident_contexts DROP COLUMN card_victim_additional_info;
ALTER TABLE incident_contexts DROP COLUMN card_victim_emotional_state;

ALTER TABLE stage_contexts ADD COLUMN victim_count INTEGER NOT NULL DEFAULT 0;
UPDATE stage_contexts
SET victim_count = 1
WHERE victim_first_name IS NOT NULL
   OR victim_last_name IS NOT NULL
   OR victim_phone IS NOT NULL;
ALTER TABLE stage_contexts
    ADD CONSTRAINT ck_stage_context_victim_count CHECK (victim_count >= 0);
ALTER TABLE stage_contexts DROP COLUMN victim_first_name;
ALTER TABLE stage_contexts DROP COLUMN victim_last_name;
ALTER TABLE stage_contexts DROP COLUMN victim_middle_name;
ALTER TABLE stage_contexts DROP COLUMN victim_age;
ALTER TABLE stage_contexts DROP COLUMN victim_phone;
ALTER TABLE stage_contexts DROP COLUMN victim_contact_phone;
ALTER TABLE stage_contexts DROP COLUMN victim_address;
ALTER TABLE stage_contexts DROP COLUMN victim_additional_info;
ALTER TABLE stage_contexts DROP COLUMN victim_emotional_state;

ALTER TABLE solution_contexts ADD COLUMN victim_count INTEGER;
UPDATE solution_contexts
SET victim_count = 1
WHERE victim_first_name IS NOT NULL
   OR victim_last_name IS NOT NULL
   OR victim_phone IS NOT NULL;
ALTER TABLE solution_contexts
    ADD CONSTRAINT ck_solution_context_victim_count CHECK (victim_count IS NULL OR victim_count >= 0);
ALTER TABLE solution_contexts DROP COLUMN victim_phone;
ALTER TABLE solution_contexts DROP COLUMN victim_contact_phone;
ALTER TABLE solution_contexts DROP COLUMN victim_last_name;
ALTER TABLE solution_contexts DROP COLUMN victim_first_name;
ALTER TABLE solution_contexts DROP COLUMN victim_middle_name;
ALTER TABLE solution_contexts DROP COLUMN victim_address;
ALTER TABLE solution_contexts DROP COLUMN victim_additional_info;
