ALTER TABLE stages ADD COLUMN classifier_code VARCHAR(50);

UPDATE stages
SET classifier_code = classifier_entries.code
FROM classifier_entries
WHERE stages.classifier_entry_id = classifier_entries.id;

ALTER TABLE stages ALTER COLUMN classifier_code SET NOT NULL;
ALTER TABLE stages DROP CONSTRAINT IF EXISTS stages_classifier_entry_id_fkey;
ALTER TABLE stages DROP COLUMN classifier_entry_id;
CREATE INDEX idx_stages_classifier_code ON stages (classifier_code);

DROP TABLE routing_rules;
DROP TABLE routing_variant_conditions;
DROP TABLE routing_variants;
DROP TABLE classifier_entry_primary_services;
DROP TABLE dispatch_services;
DROP TABLE classifier_entries;
DROP TABLE classifier_categories;
