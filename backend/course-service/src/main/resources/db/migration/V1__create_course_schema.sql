CREATE TABLE courses (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM_112',
    author_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_courses_author_id ON courses (author_id);

CREATE TABLE course_materials (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    file_object_key VARCHAR(512) NOT NULL,
    file_name VARCHAR(255),
    file_content_type VARCHAR(255),
    file_size BIGINT,
    CONSTRAINT uk_course_materials_position UNIQUE (course_id, position)
);

CREATE INDEX idx_course_materials_course_id ON course_materials (course_id);
CREATE UNIQUE INDEX uk_course_material_file_object_key ON course_materials (file_object_key);

CREATE TABLE course_assignments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    difficulty VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    execution_mode VARCHAR(32) NOT NULL DEFAULT 'SEQUENTIAL',
    CONSTRAINT uk_course_assignments_position UNIQUE (course_id, position)
);

CREATE INDEX idx_course_assignments_course_id ON course_assignments (course_id);

CREATE TABLE course_assignment_incidents (
    assignment_id UUID NOT NULL REFERENCES course_assignments (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    incident_id UUID NOT NULL,
    PRIMARY KEY (assignment_id, position)
);

CREATE TABLE study_groups (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_study_groups_owner_id ON study_groups (owner_id);

CREATE TABLE study_group_students (
    group_id UUID NOT NULL REFERENCES study_groups (id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    PRIMARY KEY (group_id, student_id)
);

CREATE TABLE course_enrollments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    group_id UUID REFERENCES study_groups (id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_course_enrollment_student UNIQUE (course_id, student_id)
);

CREATE INDEX idx_course_enrollments_student_id ON course_enrollments (student_id);
