ALTER TABLE stage_contexts ADD COLUMN IF NOT EXISTS expected_comment TEXT;
ALTER TABLE stage_contexts ADD COLUMN IF NOT EXISTS comment TEXT;

CREATE TABLE system112_stage_contexts (
    stage_context_id UUID PRIMARY KEY REFERENCES stage_contexts (id) ON DELETE CASCADE,
    victim_count INTEGER NOT NULL DEFAULT 0 CHECK (victim_count >= 0)
);

CREATE TABLE dds_stage_contexts (
    stage_context_id UUID PRIMARY KEY REFERENCES stage_contexts (id) ON DELETE CASCADE,
    dds_stage_type VARCHAR(50) NOT NULL,
    time_limit_seconds INTEGER,
    expected_comment TEXT,
    comment TEXT
);

INSERT INTO system112_stage_contexts (stage_context_id, victim_count)
SELECT id, victim_count FROM stage_contexts WHERE dds_stage_type IS NULL;

INSERT INTO dds_stage_contexts (stage_context_id, dds_stage_type, time_limit_seconds, expected_comment, comment)
SELECT id, dds_stage_type, time_limit_seconds, expected_comment, comment
FROM stage_contexts WHERE dds_stage_type IS NOT NULL;

CREATE TABLE system112_stage_classifier_codes (
    stage_context_id UUID NOT NULL REFERENCES system112_stage_contexts (stage_context_id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    classifier_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (stage_context_id, position)
);

INSERT INTO system112_stage_classifier_codes (stage_context_id, position, classifier_code)
SELECT codes.stage_context_id, codes.position, codes.classifier_code
FROM stage_context_classifier_codes codes
JOIN system112_stage_contexts details ON details.stage_context_id = codes.stage_context_id;
DROP TABLE stage_context_classifier_codes;
ALTER TABLE stage_contexts DROP CONSTRAINT ck_stage_context_victim_count;
ALTER TABLE stage_contexts DROP COLUMN victim_count;
ALTER TABLE stage_contexts DROP COLUMN dds_stage_type;
ALTER TABLE stage_contexts DROP COLUMN time_limit_seconds;
ALTER TABLE stage_contexts DROP COLUMN expected_comment;
ALTER TABLE stage_contexts DROP COLUMN comment;
