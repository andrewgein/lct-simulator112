ALTER TABLE dds_stage_contexts ADD COLUMN actual_status VARCHAR(50);
DROP TABLE IF EXISTS context_dds_stage_transitions;
ALTER TABLE incident_contexts DROP COLUMN IF EXISTS initial_stage_id;
