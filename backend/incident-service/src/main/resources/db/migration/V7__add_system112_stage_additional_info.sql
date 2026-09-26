CREATE TABLE system112_stage_additional_info (
    stage_id UUID NOT NULL REFERENCES system112_stage_details (stage_id) ON DELETE CASCADE,
    info_key VARCHAR(255) NOT NULL,
    info_value TEXT,
    PRIMARY KEY (stage_id, info_key)
);
