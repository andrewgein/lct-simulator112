ALTER TABLE incident_contexts ADD COLUMN card_applicant_on_scene_phone VARCHAR(255);
ALTER TABLE solution_contexts ADD COLUMN applicant_on_scene_phone VARCHAR(255);
ALTER TABLE call_contexts ADD COLUMN applicant_on_scene_phone VARCHAR(255);
