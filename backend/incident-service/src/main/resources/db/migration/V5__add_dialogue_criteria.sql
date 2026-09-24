CREATE TABLE incident_dialogue_criteria (
    incident_id UUID NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    criterion_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    hypothesis TEXT NOT NULL,
    weight INTEGER NOT NULL CHECK (weight BETWEEN 1 AND 40),
    PRIMARY KEY (incident_id, position),
    UNIQUE (incident_id, criterion_id)
);

-- Legacy criteria tables are intentionally retained during migration.
-- They are no longer read and can be removed after existing scenarios are recreated.
