CREATE TABLE system112_stage_expected_routing_facts (
    stage_id UUID NOT NULL REFERENCES system112_stage_details (stage_id) ON DELETE CASCADE,
    fact_code VARCHAR(255) NOT NULL,
    expected_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (stage_id, fact_code)
);
