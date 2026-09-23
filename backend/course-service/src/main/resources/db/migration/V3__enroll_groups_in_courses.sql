CREATE TABLE course_group_enrollments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES study_groups (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_course_group_enrollment UNIQUE (course_id, group_id)
);

INSERT INTO course_group_enrollments (id, course_id, group_id, created_at)
SELECT id, course_id, group_id, created_at
FROM (
    SELECT id, course_id, group_id, created_at,
           ROW_NUMBER() OVER (PARTITION BY course_id, group_id ORDER BY created_at, id) AS row_number
    FROM course_enrollments
    WHERE group_id IS NOT NULL
) enrollments
WHERE row_number = 1;

DROP TABLE course_enrollments;
ALTER TABLE course_group_enrollments RENAME TO course_enrollments;

CREATE INDEX idx_course_enrollments_group_id ON course_enrollments (group_id);
