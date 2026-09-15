CREATE TABLE types (
    id UUID PRIMARY KEY,
    service_type VARCHAR(50) NOT NULL,
    type_id VARCHAR(255) NOT NULL,
    type_name VARCHAR(255) NOT NULL
);

CREATE TABLE type_instructions (
    id UUID PRIMARY KEY,
    type_id UUID NOT NULL REFERENCES types (id) ON DELETE CASCADE,
    instructions TEXT NOT NULL,
    position INTEGER NOT NULL
);

CREATE TABLE additional_info (
    id UUID PRIMARY KEY,
    type_id UUID NOT NULL REFERENCES types (id) ON DELETE CASCADE,
    field_code VARCHAR(255) NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    required BOOLEAN NOT NULL,
    position INTEGER NOT NULL
);

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
    type_id UUID NOT NULL REFERENCES types (id),
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

CREATE TABLE stage_additional_info (
    id UUID PRIMARY KEY,
    stage_id UUID NOT NULL REFERENCES stages (id) ON DELETE CASCADE,
    additional_info_id UUID NOT NULL REFERENCES additional_info (id),
    field_value TEXT,
    CONSTRAINT uk_stage_additional_info UNIQUE (stage_id, additional_info_id)
);

CREATE INDEX idx_stages_incident_id ON stages (incident_id);

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
