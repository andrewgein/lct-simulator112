ALTER TABLE stage_contexts
    ADD COLUMN victim_contact_phone VARCHAR(255),
    ADD COLUMN victim_address VARCHAR(255),
    ADD COLUMN victim_additional_info TEXT;

ALTER TABLE dialup_contexts
    ADD COLUMN applicant_contact_phone VARCHAR(255),
    ADD COLUMN applicant_address VARCHAR(255),
    ADD COLUMN applicant_additional_info TEXT;
