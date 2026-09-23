package com.simulator112.course.domain.course;

import java.util.UUID;

public record CourseMaterial(
        UUID id,
        String title,
        String fileObjectKey,
        String fileName,
        String fileContentType,
        Long fileSize) {

    public boolean hasFile() {
        return fileObjectKey != null && !fileObjectKey.isBlank();
    }

}
