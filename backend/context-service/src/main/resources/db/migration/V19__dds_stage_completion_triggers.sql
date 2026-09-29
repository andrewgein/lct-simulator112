CREATE TABLE dds_stage_completion_triggers (
    stage_context_id UUID NOT NULL REFERENCES dds_stage_contexts(stage_context_id) ON DELETE CASCADE,
    completion_trigger VARCHAR(20) NOT NULL,
    PRIMARY KEY (stage_context_id, completion_trigger)
);

INSERT INTO dds_stage_completion_triggers(stage_context_id, completion_trigger)
SELECT stage_context_id, 'TIME' FROM dds_stage_contexts;

INSERT INTO dds_stage_completion_triggers(stage_context_id, completion_trigger)
SELECT stage_context_id, 'STATUS' FROM dds_stage_contexts WHERE dds_stage_type = 'ASSIGN_BRIGADE';
