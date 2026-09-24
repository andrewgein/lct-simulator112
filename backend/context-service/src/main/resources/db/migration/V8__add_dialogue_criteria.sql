CREATE TABLE incident_context_dialogue_criteria (
    incident_context_id UUID NOT NULL REFERENCES incident_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    criterion_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    hypothesis TEXT NOT NULL,
    weight INTEGER NOT NULL CHECK (weight BETWEEN 1 AND 40),
    PRIMARY KEY (incident_context_id, position),
    UNIQUE (incident_context_id, criterion_id)
);

-- Legacy criteria tables are intentionally retained during migration.
