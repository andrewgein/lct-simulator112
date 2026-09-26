ALTER TABLE course_assignments ADD COLUMN threshold_3 INTEGER;
ALTER TABLE course_assignments ADD COLUMN threshold_4 INTEGER;
ALTER TABLE course_assignments ADD COLUMN threshold_5 INTEGER;

ALTER TABLE course_assignments ADD CONSTRAINT ck_assignment_grade_thresholds CHECK (
    (threshold_3 IS NULL AND threshold_4 IS NULL AND threshold_5 IS NULL)
    OR (threshold_3 IS NOT NULL AND threshold_4 IS NOT NULL AND threshold_5 IS NOT NULL
        AND threshold_3 BETWEEN 0 AND 100
        AND threshold_4 > threshold_3 AND threshold_4 <= 100
        AND threshold_5 > threshold_4 AND threshold_5 <= 100)
);
