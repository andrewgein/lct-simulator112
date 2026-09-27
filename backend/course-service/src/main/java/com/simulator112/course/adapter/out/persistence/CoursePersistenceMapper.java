package com.simulator112.course.adapter.out.persistence;

import com.simulator112.course.adapter.out.persistence.entity.AssignmentJpaEntity;
import com.simulator112.course.adapter.out.persistence.entity.CourseJpaEntity;
import com.simulator112.course.adapter.out.persistence.entity.CourseMaterialJpaEntity;
import com.simulator112.course.adapter.out.persistence.entity.EnrollmentJpaEntity;
import com.simulator112.course.adapter.out.persistence.entity.StudyGroupJpaEntity;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseMaterial;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.group.StudyGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class CoursePersistenceMapper {

    public Course toDomain(CourseJpaEntity entity) {
        return new Course(entity.getId(), entity.getTitle(), entity.getDescription(), entity.getTargetType(), entity.getDdsService(),
                entity.getAuthorId(), entity.getMaterials().stream().map(this::toDomain).toList(),
                entity.getAssignments().stream().map(this::toDomain).toList(), entity.getDeletedAt());
    }

    public CourseJpaEntity toEntity(Course course) {
        CourseJpaEntity entity = new CourseJpaEntity();
        entity.setId(course.id());
        entity.setTitle(course.title());
        entity.setDescription(course.description());
        entity.setTargetType(course.targetType());
        entity.setDdsService(course.ddsService());
        entity.setAuthorId(course.authorId());
        entity.setDeletedAt(course.deletedAt());
        course.materials().stream().map(this::toEntity).forEach(entity::addMaterial);
        course.assignments().stream().map(this::toEntity).forEach(entity::addAssignment);
        return entity;
    }

    public StudyGroup toDomain(StudyGroupJpaEntity entity) {
        return new StudyGroup(entity.getId(), entity.getTitle(), entity.getOwnerId(), entity.getStudentIds());
    }

    public StudyGroupJpaEntity toEntity(StudyGroup studyGroup) {
        StudyGroupJpaEntity entity = new StudyGroupJpaEntity();
        entity.setId(studyGroup.id());
        entity.setTitle(studyGroup.title());
        entity.setOwnerId(studyGroup.ownerId());
        entity.setStudentIds(new ArrayList<>(studyGroup.studentIds()));
        return entity;
    }

    public Enrollment toDomain(EnrollmentJpaEntity entity) {
        return new Enrollment(entity.getId(), entity.getCourseId(), entity.getGroupId());
    }

    public EnrollmentJpaEntity toEntity(Enrollment enrollment) {
        EnrollmentJpaEntity entity = new EnrollmentJpaEntity();
        entity.setId(enrollment.id());
        entity.setCourseId(enrollment.courseId());
        entity.setGroupId(enrollment.groupId());
        return entity;
    }

    private CourseMaterial toDomain(CourseMaterialJpaEntity entity) {
        return new CourseMaterial(entity.getId(), entity.getTitle(), entity.getFileObjectKey(),
                entity.getFileName(), entity.getFileContentType(), entity.getFileSize());
    }

    private CourseMaterialJpaEntity toEntity(CourseMaterial material) {
        CourseMaterialJpaEntity entity = new CourseMaterialJpaEntity();
        entity.setId(material.id());
        entity.setTitle(material.title());
        entity.setFileObjectKey(material.fileObjectKey());
        entity.setFileName(material.fileName());
        entity.setFileContentType(material.fileContentType());
        entity.setFileSize(material.fileSize());
        return entity;
    }

    private Assignment toDomain(AssignmentJpaEntity entity) {
        return new Assignment(entity.getId(), entity.getTitle(), entity.getDescription(), entity.getDifficulty(),
                entity.getExecutionMode(), entity.getIncidentIds(), entity.getThreshold3(), entity.getThreshold4(),
                entity.getThreshold5());
    }

    private AssignmentJpaEntity toEntity(Assignment assignment) {
        AssignmentJpaEntity entity = new AssignmentJpaEntity();
        entity.setId(assignment.id());
        entity.setTitle(assignment.title());
        entity.setDescription(assignment.description());
        entity.setDifficulty(assignment.difficulty());
        entity.setExecutionMode(assignment.executionMode());
        entity.setThreshold3(assignment.threshold3());
        entity.setThreshold4(assignment.threshold4());
        entity.setThreshold5(assignment.threshold5());
        entity.setIncidentIds(new ArrayList<>(assignment.incidentIds()));
        return entity;
    }
}
