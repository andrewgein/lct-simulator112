package com.simulator112.review_service.application.port.out;

import java.util.UUID;

public interface TeacherStudentAccessPort {
    /** @return true, если studentId учится в одной из учебных групп, которыми владеет teacherId */
    boolean isStudentOfTeacher(UUID teacherId, UUID studentId);
}
