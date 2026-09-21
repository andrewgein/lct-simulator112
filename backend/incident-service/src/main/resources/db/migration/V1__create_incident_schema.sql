CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    address_city VARCHAR(255),
    address_street VARCHAR(255),
    address_house VARCHAR(255),
    address_building VARCHAR(255),
    address_apartment VARCHAR(255),
    address_floor INTEGER,
    emergency_service VARCHAR(50),
    dds_initial_stage_id UUID,
    prepared_card_classifier_code VARCHAR(50),
    initial_assignment_classifier_code VARCHAR(50),
    initial_assignment_instructions TEXT,
    card_applicant_first_name VARCHAR(255),
    card_applicant_last_name VARCHAR(255),
    card_applicant_middle_name VARCHAR(255),
    card_applicant_age INTEGER,
    card_applicant_phone VARCHAR(255),
    card_applicant_contact_phone VARCHAR(255),
    card_applicant_address VARCHAR(255),
    card_applicant_additional_info TEXT,
    card_victim_first_name VARCHAR(255),
    card_victim_last_name VARCHAR(255),
    card_victim_middle_name VARCHAR(255),
    card_victim_age INTEGER,
    card_victim_phone VARCHAR(255),
    card_victim_contact_phone VARCHAR(255),
    card_victim_address VARCHAR(255),
    card_victim_additional_info TEXT,
    CONSTRAINT ck_incident_target_type CHECK (target_type IN ('SYSTEM_112', 'DDS')),
    CONSTRAINT ck_incident_difficulty CHECK (difficulty IN ('EASY', 'NORMAL', 'HARD')),
    CONSTRAINT ck_incident_emergency_service CHECK (emergency_service IS NULL OR emergency_service IN
        ('FIRE', 'POLICE', 'AMBULANCE', 'GAS', 'ANTI_TERROR')),
    CONSTRAINT ck_incident_profile CHECK (
        (target_type = 'SYSTEM_112' AND emergency_service IS NULL AND prepared_card_classifier_code IS NULL)
        OR
        (target_type = 'DDS' AND emergency_service IS NOT NULL AND prepared_card_classifier_code IS NOT NULL)
    )
);

CREATE INDEX idx_incidents_availability ON incidents (target_type, difficulty);

CREATE TABLE levels (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    execution_mode VARCHAR(20) NOT NULL,
    CONSTRAINT ck_level_target_type CHECK (target_type IN ('SYSTEM_112', 'DDS')),
    CONSTRAINT ck_level_difficulty CHECK (difficulty IN ('EASY', 'NORMAL', 'HARD')),
    CONSTRAINT ck_level_execution_mode CHECK (execution_mode IN ('SEQUENTIAL', 'PARALLEL'))
);

CREATE TABLE level_incidents (
    level_id UUID NOT NULL REFERENCES levels (id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents (id),
    position INTEGER NOT NULL,
    PRIMARY KEY (level_id, position),
    CONSTRAINT uk_level_incident UNIQUE (level_id, incident_id)
);

CREATE INDEX idx_level_incidents_incident_id ON level_incidents (incident_id);

CREATE TABLE incident_stages (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER,
    title VARCHAR(255),
    description TEXT,
    CONSTRAINT uk_incident_stages_position UNIQUE (incident_id, position),
    CONSTRAINT uk_incident_stages_incident_and_id UNIQUE (incident_id, id)
);

CREATE INDEX idx_incident_stages_incident_id ON incident_stages (incident_id);

CREATE TABLE system112_stage_details (
    stage_id UUID PRIMARY KEY REFERENCES incident_stages (id) ON DELETE CASCADE,
    classifier_code VARCHAR(50) NOT NULL,
    victim_first_name VARCHAR(255),
    victim_last_name VARCHAR(255),
    victim_middle_name VARCHAR(255),
    victim_age INTEGER,
    victim_phone VARCHAR(255),
    victim_contact_phone VARCHAR(255),
    victim_address VARCHAR(255),
    victim_additional_info TEXT
);

CREATE INDEX idx_system112_stage_details_classifier_code ON system112_stage_details (classifier_code);

CREATE TABLE dds_stage_details (
    stage_id UUID PRIMARY KEY REFERENCES incident_stages (id) ON DELETE CASCADE,
    stage_type VARCHAR(50) NOT NULL,
    time_limit_seconds INTEGER NOT NULL,
    CONSTRAINT ck_dds_stage_type CHECK (stage_type IN (
        'ASSIGN_BRIGADE',
        'WAIT_FOR_BRIGADE_STATUS_CHANGE',
        'CALL_BRIGADE_FOR_STATUS',
        'REQUEST_ADDITIONAL_SERVICE',
        'COMPLETE_INCIDENT'
    )),
    CONSTRAINT ck_dds_stage_time_limit CHECK (time_limit_seconds > 0)
);

CREATE TABLE dds_stage_transitions (
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    stage_id UUID NOT NULL,
    success_stage_id UUID,
    failure_stage_id UUID,
    PRIMARY KEY (incident_id, stage_id),
    CONSTRAINT fk_dds_transition_stage FOREIGN KEY (incident_id, stage_id)
        REFERENCES incident_stages (incident_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_dds_transition_success FOREIGN KEY (incident_id, success_stage_id)
        REFERENCES incident_stages (incident_id, id),
    CONSTRAINT fk_dds_transition_failure FOREIGN KEY (incident_id, failure_stage_id)
        REFERENCES incident_stages (incident_id, id),
    CONSTRAINT fk_dds_transition_stage_type FOREIGN KEY (stage_id)
        REFERENCES dds_stage_details (stage_id),
    CONSTRAINT fk_dds_transition_success_type FOREIGN KEY (success_stage_id)
        REFERENCES dds_stage_details (stage_id),
    CONSTRAINT fk_dds_transition_failure_type FOREIGN KEY (failure_stage_id)
        REFERENCES dds_stage_details (stage_id)
);

CREATE TABLE call_scenarios (
    id UUID PRIMARY KEY,
    stage_id UUID NOT NULL REFERENCES incident_stages (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    direction VARCHAR(20) NOT NULL,
    counterparty VARCHAR(20) NOT NULL,
    gender VARCHAR(20),
    ai_context TEXT,
    emotional_state VARCHAR(255),
    person_first_name VARCHAR(255),
    person_last_name VARCHAR(255),
    person_middle_name VARCHAR(255),
    person_age INTEGER,
    person_phone VARCHAR(255),
    person_contact_phone VARCHAR(255),
    person_address VARCHAR(255),
    person_additional_info TEXT,
    CONSTRAINT ck_call_direction CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    CONSTRAINT ck_call_counterparty CHECK (counterparty IN ('CALLER', 'BRIGADE')),
    CONSTRAINT uk_call_scenarios_position UNIQUE (stage_id, position)
);

CREATE INDEX idx_call_scenarios_stage_id ON call_scenarios (stage_id);

CREATE TABLE call_scenario_known_facts (
    call_id UUID NOT NULL REFERENCES call_scenarios (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    fact TEXT NOT NULL,
    PRIMARY KEY (call_id, position)
);

CREATE TABLE call_scenario_hidden_facts (
    call_id UUID NOT NULL REFERENCES call_scenarios (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    fact TEXT NOT NULL,
    PRIMARY KEY (call_id, position)
);

CREATE TABLE incident_required_questions (
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    question TEXT NOT NULL,
    PRIMARY KEY (incident_id, position)
);

CREATE TABLE incident_expected_actions (
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    action TEXT NOT NULL,
    PRIMARY KEY (incident_id, position)
);

CREATE TABLE incident_critical_mistakes (
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    mistake TEXT NOT NULL,
    PRIMARY KEY (incident_id, position)
);

CREATE TABLE prepared_card_additional_info (
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    info_key VARCHAR(255) NOT NULL,
    info_value TEXT,
    PRIMARY KEY (incident_id, info_key)
);
