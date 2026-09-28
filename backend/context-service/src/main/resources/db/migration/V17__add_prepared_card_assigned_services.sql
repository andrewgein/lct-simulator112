CREATE TABLE context_prepared_card_assigned_services (
    incident_context_id UUID NOT NULL REFERENCES incident_contexts(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    service_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (incident_context_id, position)
);

INSERT INTO context_prepared_card_assigned_services (incident_context_id, position, service_code)
SELECT id, 0, initial_assignment_service FROM incident_contexts WHERE initial_assignment_service IS NOT NULL AND initial_assignment_service <> '';
