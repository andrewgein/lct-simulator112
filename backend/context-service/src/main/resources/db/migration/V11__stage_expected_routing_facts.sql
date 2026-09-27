CREATE TABLE stage_context_expected_routing_facts (
    stage_context_id UUID NOT NULL REFERENCES stage_contexts (id) ON DELETE CASCADE,
    fact_code VARCHAR(255) NOT NULL,
    expected_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (stage_context_id, fact_code)
);
