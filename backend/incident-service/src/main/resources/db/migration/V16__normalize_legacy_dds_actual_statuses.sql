-- Statuses of incident cards used to be selectable as DDS reaction statuses.
-- Only COMPLETED has an unambiguous reaction-status equivalent.
UPDATE dds_stage_details
SET actual_status = 'WORK_COMPLETED'
WHERE actual_status = 'COMPLETED';

-- If a non-initial stage relied solely on a status that is being cleared,
-- let it finish by time instead of leaving it without a completion trigger.
INSERT INTO dds_stage_completion_triggers (stage_id, completion_trigger)
SELECT details.stage_id, 'TIME'
FROM dds_stage_details details
WHERE details.stage_type <> 'ASSIGN_BRIGADE'
  AND details.actual_status IN ('REGISTERED', 'PROCESSED', 'VERIFIED', 'NOT_NOTIFIED', 'REFUSED', 'NOT_COMPLETED')
  AND EXISTS (SELECT 1 FROM dds_stage_completion_triggers t
              WHERE t.stage_id = details.stage_id AND t.completion_trigger = 'STATUS')
  AND NOT EXISTS (SELECT 1 FROM dds_stage_completion_triggers t
                  WHERE t.stage_id = details.stage_id AND t.completion_trigger <> 'STATUS');

DELETE FROM dds_stage_completion_triggers
WHERE completion_trigger = 'STATUS'
  AND stage_id IN (
      SELECT stage_id FROM dds_stage_details
      WHERE stage_type <> 'ASSIGN_BRIGADE'
        AND actual_status IN ('REGISTERED', 'PROCESSED', 'VERIFIED', 'NOT_NOTIFIED', 'REFUSED', 'NOT_COMPLETED')
  );

UPDATE dds_stage_details
SET actual_status = NULL
WHERE actual_status IN ('REGISTERED', 'PROCESSED', 'VERIFIED', 'NOT_NOTIFIED', 'REFUSED', 'NOT_COMPLETED');
