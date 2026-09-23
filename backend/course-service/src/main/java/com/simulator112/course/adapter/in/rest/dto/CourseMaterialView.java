package com.simulator112.course.adapter.in.rest.dto;

import java.util.UUID;

public record CourseMaterialView(
        UUID id,
        int position,
        String title,
        String fileObjectKey,
        String fileName,
        String fileContentType,
        Long fileSize,
        String downloadUrl) {
}
