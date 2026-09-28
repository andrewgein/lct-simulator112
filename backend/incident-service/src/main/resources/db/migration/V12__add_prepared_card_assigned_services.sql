CREATE TABLE prepared_card_assigned_services (
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    service_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (incident_id, position)
);

INSERT INTO prepared_card_assigned_services (incident_id, position, service_code)
SELECT id, 0, emergency_service FROM incidents WHERE emergency_service IS NOT NULL AND emergency_service <> '';
