CREATE TABLE dds_stage_completion_triggers (
    stage_id UUID NOT NULL REFERENCES dds_stage_details(stage_id) ON DELETE CASCADE,
    completion_trigger VARCHAR(20) NOT NULL,
    PRIMARY KEY (stage_id, completion_trigger)
);

INSERT INTO dds_stage_completion_triggers(stage_id, completion_trigger)
SELECT stage_id, 'TIME' FROM dds_stage_details;

INSERT INTO dds_stage_completion_triggers(stage_id, completion_trigger)
SELECT stage_id, 'STATUS' FROM dds_stage_details WHERE stage_type = 'ASSIGN_BRIGADE';
