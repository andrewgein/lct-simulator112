-- The ordered request lists determine positions; replacement during a Hibernate flush
-- may temporarily leave old and new children at the same position.
ALTER TABLE course_materials DROP CONSTRAINT uk_course_materials_position;
ALTER TABLE course_assignments DROP CONSTRAINT uk_course_assignments_position;
