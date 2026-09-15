CREATE TABLE contexts (
    uuid UUID PRIMARY KEY,
    level_id UUID,
    difficulty VARCHAR(50),
    user_id UUID,
    status VARCHAR(255),
    active_dialup_id UUID,
    dialog_status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE incident_contexts (
    id UUID PRIMARY KEY,
    context_id UUID NOT NULL REFERENCES contexts (uuid) ON DELETE CASCADE,
    source_incident_id VARCHAR(255),
    title VARCHAR(255),
    address_city VARCHAR(255),
    address_street VARCHAR(255),
    address_house VARCHAR(255),
    address_building VARCHAR(255),
    address_apartment VARCHAR(255),
    address_floor INTEGER,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_incident_context_source UNIQUE (context_id, source_incident_id)
);

CREATE TABLE incident_context_required_questions (
    incident_context_id UUID NOT NULL REFERENCES incident_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    question VARCHAR(255) NOT NULL,
    PRIMARY KEY (incident_context_id, position)
);

CREATE TABLE incident_context_expected_actions (
    incident_context_id UUID NOT NULL REFERENCES incident_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    action VARCHAR(255) NOT NULL,
    PRIMARY KEY (incident_context_id, position)
);

CREATE TABLE incident_context_critical_mistakes (
    incident_context_id UUID NOT NULL REFERENCES incident_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    mistake VARCHAR(255) NOT NULL,
    PRIMARY KEY (incident_context_id, position)
);

CREATE TABLE solution_contexts (
    id UUID PRIMARY KEY,
    context_id UUID NOT NULL REFERENCES contexts (uuid) ON DELETE CASCADE,
    card_id UUID NOT NULL,
    previous_revision_id UUID REFERENCES solution_contexts (id),
    version BIGINT NOT NULL CHECK (version > 0),
    status VARCHAR(50) NOT NULL,
    dialup_id UUID NOT NULL,
    parent_card_id UUID,
    duplicate_of_card_id UUID,
    applicant_phone VARCHAR(255),
    applicant_contact_phone VARCHAR(255),
    applicant_last_name VARCHAR(255),
    applicant_first_name VARCHAR(255),
    applicant_middle_name VARCHAR(255),
    applicant_address VARCHAR(255),
    applicant_additional_info TEXT,
    victim_phone VARCHAR(255),
    victim_contact_phone VARCHAR(255),
    victim_last_name VARCHAR(255),
    victim_first_name VARCHAR(255),
    victim_middle_name VARCHAR(255),
    victim_address VARCHAR(255),
    victim_additional_info TEXT,
    additional_info_provided BOOLEAN NOT NULL,
    incident_type VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_solution_context_card_version UNIQUE (context_id, card_id, version),
    CONSTRAINT uk_solution_context_previous_revision UNIQUE (previous_revision_id),
    CONSTRAINT ck_solution_context_relation CHECK (
        NOT (parent_card_id IS NOT NULL AND duplicate_of_card_id IS NOT NULL)
    )
);

CREATE INDEX idx_solution_context_context_dialup
    ON solution_contexts (context_id, dialup_id);
CREATE INDEX idx_solution_context_context_card_version
    ON solution_contexts (context_id, card_id, version DESC);

CREATE TABLE solution_context_additional_info (
    solution_context_id UUID NOT NULL REFERENCES solution_contexts (id) ON DELETE CASCADE,
    info_key VARCHAR(255) NOT NULL,
    info_value TEXT,
    PRIMARY KEY (solution_context_id, info_key)
);

CREATE TABLE stage_contexts (
    id UUID PRIMARY KEY,
    source_stage_id UUID NOT NULL,
    incident_context_id UUID NOT NULL REFERENCES incident_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    title VARCHAR(255),
    type_ref_id VARCHAR(255),
    type_business_id VARCHAR(255),
    type_service_type VARCHAR(50),
    type_name VARCHAR(255),
    description TEXT,
    victim_first_name VARCHAR(255),
    victim_last_name VARCHAR(255),
    victim_middle_name VARCHAR(255),
    victim_age INTEGER,
    victim_phone VARCHAR(255),
    victim_emotional_state VARCHAR(50),
    CONSTRAINT uk_stage_context_source UNIQUE (incident_context_id, source_stage_id)
);

CREATE TABLE stage_context_additional_info (
    id UUID PRIMARY KEY,
    stage_context_id UUID NOT NULL REFERENCES stage_contexts (id) ON DELETE CASCADE,
    source_additional_info_id VARCHAR(255),
    field_code VARCHAR(255),
    field_name VARCHAR(255),
    field_type VARCHAR(50),
    required BOOLEAN NOT NULL DEFAULT FALSE,
    field_value TEXT
);

CREATE TABLE dialup_contexts (
    id UUID PRIMARY KEY,
    source_dialup_id UUID NOT NULL,
    stage_context_id UUID NOT NULL REFERENCES stage_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    queue_position INTEGER NOT NULL,
    ai_context TEXT,
    gender VARCHAR(20),
    emotional_state VARCHAR(255),
    applicant_first_name VARCHAR(255),
    applicant_last_name VARCHAR(255),
    applicant_middle_name VARCHAR(255),
    applicant_age INTEGER,
    applicant_phone VARCHAR(255),
    applicant_emotional_state VARCHAR(50),
    CONSTRAINT uk_dialup_context_source UNIQUE (stage_context_id, source_dialup_id)
);

CREATE INDEX idx_dialup_context_queue_position
    ON dialup_contexts (queue_position);

CREATE TABLE dialup_context_known_facts (
    dialup_context_id UUID NOT NULL REFERENCES dialup_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    fact VARCHAR(255) NOT NULL,
    PRIMARY KEY (dialup_context_id, position)
);

CREATE TABLE dialup_context_hidden_facts (
    dialup_context_id UUID NOT NULL REFERENCES dialup_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    fact VARCHAR(255) NOT NULL,
    PRIMARY KEY (dialup_context_id, position)
);

CREATE TABLE dialog_contexts (
    context_id UUID PRIMARY KEY REFERENCES contexts (uuid) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE dialog_phrases (
    context_id UUID NOT NULL REFERENCES dialog_contexts (context_id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    speaker VARCHAR(20) NOT NULL,
    text TEXT NOT NULL,
    PRIMARY KEY (context_id, position)
);
