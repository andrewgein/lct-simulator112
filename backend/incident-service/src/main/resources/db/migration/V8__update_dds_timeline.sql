ALTER TABLE dds_stage_details ADD COLUMN actual_status VARCHAR(50);
DROP TABLE IF EXISTS dds_stage_transitions;
ALTER TABLE incidents DROP COLUMN IF EXISTS dds_initial_stage_id;
