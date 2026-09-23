CREATE TABLE context_service_reactions (
    id UUID PRIMARY KEY,
    incident_context_id UUID NOT NULL REFERENCES incident_contexts (id) ON DELETE CASCADE,
    service_code VARCHAR(100) NOT NULL,
    CONSTRAINT uk_context_service_reaction UNIQUE (incident_context_id, service_code)
);

CREATE TABLE context_service_reaction_history (
    service_reaction_id UUID NOT NULL REFERENCES context_service_reactions (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    reaction_status VARCHAR(40) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    comment TEXT,
    PRIMARY KEY (service_reaction_id, position)
);
