CREATE TABLE solution_context_services (
    solution_context_id UUID NOT NULL REFERENCES solution_contexts (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    service_code VARCHAR(100) NOT NULL,
    PRIMARY KEY (solution_context_id, position)
);
