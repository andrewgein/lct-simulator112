CREATE TABLE classifier_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL UNIQUE
);

CREATE TABLE classifier_entries (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES classifier_categories (id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    feature_1_code VARCHAR(50),
    feature_1_name TEXT,
    feature_2_code VARCHAR(50),
    feature_2_name TEXT,
    feature_3_code VARCHAR(50),
    feature_3_name TEXT,
    statistical_group TEXT,
    additional_features TEXT,
    final_name TEXT NOT NULL,
    ekp_35_name TEXT,
    primary_service_raw TEXT,
    position INTEGER NOT NULL,
    UNIQUE (category_id, position)
);
CREATE INDEX idx_classifier_entries_category_id ON classifier_entries (category_id);

CREATE TABLE dispatch_services (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE classifier_entry_primary_services (
    classifier_entry_id UUID NOT NULL REFERENCES classifier_entries (id) ON DELETE CASCADE,
    dispatch_service_id UUID NOT NULL REFERENCES dispatch_services (id) ON DELETE CASCADE,
    PRIMARY KEY (classifier_entry_id, dispatch_service_id)
);

CREATE TABLE routing_variants (
    id UUID PRIMARY KEY,
    dispatch_service_id UUID NOT NULL REFERENCES dispatch_services (id) ON DELETE CASCADE,
    routing_target TEXT NOT NULL,
    source_column VARCHAR(3) NOT NULL UNIQUE,
    header_level_1 TEXT,
    header_level_2 TEXT,
    header_level_3 TEXT,
    priority INTEGER NOT NULL,
    position INTEGER NOT NULL UNIQUE
);
CREATE INDEX idx_routing_variants_dispatch_service_id ON routing_variants (dispatch_service_id);

CREATE TABLE routing_variant_conditions (
    id UUID PRIMARY KEY,
    routing_variant_id UUID NOT NULL REFERENCES routing_variants (id) ON DELETE CASCADE,
    fact_code VARCHAR(100) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    expected_value VARCHAR(255),
    position INTEGER NOT NULL,
    UNIQUE (routing_variant_id, position)
);
CREATE INDEX idx_routing_variant_conditions_variant_id ON routing_variant_conditions (routing_variant_id);

CREATE TABLE routing_rules (
    id UUID PRIMARY KEY,
    classifier_entry_id UUID NOT NULL REFERENCES classifier_entries (id) ON DELETE CASCADE,
    routing_variant_id UUID NOT NULL REFERENCES routing_variants (id) ON DELETE CASCADE,
    result_kind VARCHAR(50) NOT NULL,
    target_type_name TEXT,
    raw_value TEXT NOT NULL,
    UNIQUE (classifier_entry_id, routing_variant_id)
);
CREATE INDEX idx_routing_rules_classifier_entry_id ON routing_rules (classifier_entry_id);
CREATE INDEX idx_routing_rules_routing_variant_id ON routing_rules (routing_variant_id);
