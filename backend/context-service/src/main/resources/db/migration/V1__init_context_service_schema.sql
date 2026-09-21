CREATE TABLE contexts (
    uuid UUID PRIMARY KEY,
    level_id UUID NOT NULL,
    level_title VARCHAR(255) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    execution_mode VARCHAR(20) NOT NULL,
    user_id UUID,
    status VARCHAR(255),
    active_call_id UUID,
    dialog_status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE incident_contexts (
    id UUID PRIMARY KEY,
    context_id UUID NOT NULL REFERENCES contexts (uuid) ON DELETE CASCADE,
    source_incident_id UUID NOT NULL,
    position INTEGER NOT NULL,
    title VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    active_stage_id UUID,
    initial_stage_id UUID,
    address_city VARCHAR(255),
    address_street VARCHAR(255),
    address_house VARCHAR(255),
    address_building VARCHAR(255),
    address_apartment VARCHAR(255),
    address_floor INTEGER,
    prepared_card_classifier_code VARCHAR(50),
    card_applicant_first_name VARCHAR(255),
    card_applicant_last_name VARCHAR(255),
    card_applicant_middle_name VARCHAR(255),
    card_applicant_age INTEGER,
    card_applicant_phone VARCHAR(255),
    card_applicant_contact_phone VARCHAR(255),
    card_applicant_address VARCHAR(255),
    card_applicant_additional_info TEXT,
    card_applicant_emotional_state VARCHAR(50),
    card_victim_first_name VARCHAR(255),
    card_victim_last_name VARCHAR(255),
    card_victim_middle_name VARCHAR(255),
    card_victim_age INTEGER,
    card_victim_phone VARCHAR(255),
    card_victim_contact_phone VARCHAR(255),
    card_victim_address VARCHAR(255),
    card_victim_additional_info TEXT,
    card_victim_emotional_state VARCHAR(50),
    initial_assignment_service VARCHAR(100),
    initial_assignment_classifier_code VARCHAR(50),
    initial_assignment_instructions TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_incident_context_source UNIQUE (context_id, source_incident_id)
);

CREATE TABLE context_prepared_card_additional_info (
    incident_context_id UUID NOT NULL REFERENCES incident_contexts (id) ON DELETE CASCADE,
    info_key VARCHAR(255) NOT NULL,
    info_value TEXT,
    PRIMARY KEY (incident_context_id, info_key)
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
    call_id UUID NOT NULL,
    main_card_id UUID,
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
    CONSTRAINT uk_solution_context_previous_revision UNIQUE (previous_revision_id)
);

CREATE INDEX idx_solution_context_context_call
    ON solution_contexts (context_id, call_id);
CREATE INDEX idx_solution_context_context_card_version
    ON solution_contexts (context_id, card_id, version DESC);

CREATE TABLE solution_context_additional_info (
    solution_context_id UUID NOT NULL REFERENCES solution_contexts (id) ON DELETE CASCADE,
    info_key VARCHAR(255) NOT NULL,
    info_value TEXT,
    PRIMARY KEY (solution_context_id, info_key)
);

CREATE TABLE context_dds_stage_transitions (
    incident_context_id UUID NOT NULL REFERENCES incident_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    stage_id UUID NOT NULL,
    success_stage_id UUID,
    failure_stage_id UUID,
    PRIMARY KEY (incident_context_id, position)
);

CREATE TABLE stage_contexts (
    id UUID PRIMARY KEY,
    source_stage_id UUID NOT NULL,
    incident_context_id UUID NOT NULL REFERENCES incident_contexts (id) ON DELETE CASCADE,
    position INTEGER,
    title VARCHAR(255),
    classifier_code VARCHAR(50),
    dds_stage_type VARCHAR(50),
    time_limit_seconds INTEGER,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    deadline_at TIMESTAMP WITH TIME ZONE,
    description TEXT,
    victim_first_name VARCHAR(255),
    victim_last_name VARCHAR(255),
    victim_middle_name VARCHAR(255),
    victim_age INTEGER,
    victim_phone VARCHAR(255),
    victim_contact_phone VARCHAR(255),
    victim_address VARCHAR(255),
    victim_additional_info TEXT,
    victim_emotional_state VARCHAR(50),
    CONSTRAINT uk_stage_context_source UNIQUE (incident_context_id, source_stage_id)
);

CREATE TABLE call_contexts (
    id UUID PRIMARY KEY,
    source_call_id UUID NOT NULL,
    stage_context_id UUID NOT NULL REFERENCES stage_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    queue_position INTEGER,
    direction VARCHAR(20) NOT NULL,
    counterparty VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ai_context TEXT,
    gender VARCHAR(20),
    emotional_state VARCHAR(255),
    applicant_first_name VARCHAR(255),
    applicant_last_name VARCHAR(255),
    applicant_middle_name VARCHAR(255),
    applicant_age INTEGER,
    applicant_phone VARCHAR(255),
    applicant_contact_phone VARCHAR(255),
    applicant_address VARCHAR(255),
    applicant_additional_info TEXT,
    applicant_emotional_state VARCHAR(50),
    CONSTRAINT uk_call_context_source UNIQUE (stage_context_id, source_call_id)
);

CREATE INDEX idx_call_context_queue_position
    ON call_contexts (queue_position);

CREATE TABLE call_context_known_facts (
    call_context_id UUID NOT NULL REFERENCES call_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    fact VARCHAR(255) NOT NULL,
    PRIMARY KEY (call_context_id, position)
);

CREATE TABLE call_context_hidden_facts (
    call_context_id UUID NOT NULL REFERENCES call_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    fact VARCHAR(255) NOT NULL,
    PRIMARY KEY (call_context_id, position)
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
