CREATE TABLE classifier_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT uk_classifier_categories_code UNIQUE (code),
    CONSTRAINT uk_classifier_categories_position UNIQUE (position)
);

CREATE TABLE classifier_entries (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES classifier_categories (id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    feature_1_code VARCHAR(50),
    feature_1_name TEXT,
    feature_2_code VARCHAR(50),
    feature_2_name TEXT,
    feature_3_code VARCHAR(50),
    feature_3_name TEXT,
    statistical_group TEXT,
    additional_features TEXT,
    final_name TEXT NOT NULL,
    ekp_35_name TEXT,
    primary_service_raw TEXT,
    position INTEGER NOT NULL,
    CONSTRAINT uk_classifier_entries_code UNIQUE (code),
    CONSTRAINT uk_classifier_entries_category_position UNIQUE (category_id, position)
);

CREATE INDEX idx_classifier_entries_category_id ON classifier_entries (category_id);

CREATE TABLE dispatch_services (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uk_dispatch_services_code UNIQUE (code)
);

CREATE TABLE classifier_entry_primary_services (
    classifier_entry_id UUID NOT NULL REFERENCES classifier_entries (id) ON DELETE CASCADE,
    dispatch_service_id UUID NOT NULL REFERENCES dispatch_services (id) ON DELETE CASCADE,
    PRIMARY KEY (classifier_entry_id, dispatch_service_id)
);

CREATE TABLE routing_variants (
    id UUID PRIMARY KEY,
    dispatch_service_id UUID NOT NULL REFERENCES dispatch_services (id) ON DELETE CASCADE,
    source_column VARCHAR(3) NOT NULL,
    header_level_1 TEXT,
    header_level_2 TEXT,
    header_level_3 TEXT,
    priority INTEGER NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT uk_routing_variants_source_column UNIQUE (source_column),
    CONSTRAINT uk_routing_variants_position UNIQUE (position)
);

CREATE INDEX idx_routing_variants_dispatch_service_id ON routing_variants (dispatch_service_id);

CREATE TABLE routing_variant_conditions (
    id UUID PRIMARY KEY,
    routing_variant_id UUID NOT NULL REFERENCES routing_variants (id) ON DELETE CASCADE,
    fact_code VARCHAR(100) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    expected_value VARCHAR(255),
    position INTEGER NOT NULL,
    CONSTRAINT uk_routing_variant_conditions_position UNIQUE (routing_variant_id, position)
);

CREATE INDEX idx_routing_variant_conditions_variant_id ON routing_variant_conditions (routing_variant_id);

CREATE TABLE routing_rules (
    id UUID PRIMARY KEY,
    classifier_entry_id UUID NOT NULL REFERENCES classifier_entries (id) ON DELETE CASCADE,
    routing_variant_id UUID NOT NULL REFERENCES routing_variants (id) ON DELETE CASCADE,
    result_kind VARCHAR(50) NOT NULL,
    target_type_name TEXT,
    raw_value TEXT NOT NULL,
    CONSTRAINT uk_routing_rules_entry_variant UNIQUE (classifier_entry_id, routing_variant_id)
);

CREATE INDEX idx_routing_rules_classifier_entry_id ON routing_rules (classifier_entry_id);
CREATE INDEX idx_routing_rules_routing_variant_id ON routing_rules (routing_variant_id);

CREATE TABLE levels (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50) NOT NULL
);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(255),
    level_id UUID REFERENCES levels (id) ON DELETE SET NULL,
    address_city VARCHAR(255),
    address_street VARCHAR(255),
    address_house VARCHAR(255),
    address_building VARCHAR(255),
    address_apartment VARCHAR(255),
    address_floor INTEGER
);

CREATE INDEX idx_incidents_level_id ON incidents (level_id);

CREATE TABLE stages (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    title VARCHAR(255),
    classifier_entry_id UUID NOT NULL REFERENCES classifier_entries (id),
    description TEXT,
    victim_first_name VARCHAR(255),
    victim_last_name VARCHAR(255),
    victim_middle_name VARCHAR(255),
    victim_age INTEGER,
    victim_phone VARCHAR(255),
    victim_contact_phone VARCHAR(255),
    victim_address VARCHAR(255),
    victim_additional_info TEXT,
    CONSTRAINT uk_stages_incident_position UNIQUE (incident_id, position)
);

CREATE INDEX idx_stages_incident_id ON stages (incident_id);
CREATE INDEX idx_stages_classifier_entry_id ON stages (classifier_entry_id);

CREATE TABLE dialups (
    id UUID PRIMARY KEY,
    stage_id UUID NOT NULL REFERENCES stages (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    gender VARCHAR(255),
    ai_context TEXT,
    emotional_state VARCHAR(255),
    applicant_first_name VARCHAR(255),
    applicant_last_name VARCHAR(255),
    applicant_middle_name VARCHAR(255),
    applicant_age INTEGER,
    applicant_phone VARCHAR(255),
    applicant_contact_phone VARCHAR(255),
    applicant_address VARCHAR(255),
    applicant_additional_info TEXT,
    CONSTRAINT uk_dialups_stage_position UNIQUE (stage_id, position)
);

CREATE INDEX idx_dialups_stage_id ON dialups (stage_id);

CREATE TABLE dialup_known_facts (
    dialup_id UUID NOT NULL REFERENCES dialups (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    fact VARCHAR(255) NOT NULL,
    PRIMARY KEY (dialup_id, position)
);

CREATE TABLE dialup_hidden_facts (
    dialup_id UUID NOT NULL REFERENCES dialups (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    fact VARCHAR(255) NOT NULL,
    PRIMARY KEY (dialup_id, position)
);

CREATE TABLE incident_required_questions (
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    question VARCHAR(255) NOT NULL,
    PRIMARY KEY (incident_id, position)
);

CREATE TABLE incident_expected_actions (
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    action VARCHAR(255) NOT NULL,
    PRIMARY KEY (incident_id, position)
);

CREATE TABLE incident_critical_mistakes (
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    mistake VARCHAR(255) NOT NULL,
    PRIMARY KEY (incident_id, position)
);
