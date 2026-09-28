ALTER TABLE call_scenarios ADD COLUMN service_code VARCHAR(100);
ALTER TABLE call_scenarios DROP CONSTRAINT ck_call_counterparty;
ALTER TABLE call_scenarios ADD CONSTRAINT ck_call_counterparty CHECK (counterparty IN ('CALLER', 'BRIGADE', 'SERVICE'));
