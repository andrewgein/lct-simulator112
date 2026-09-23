package com.simulator112.course.adapter.in.rest.dto;

public record MaterialFileUploadView(
        String fileObjectKey,
        String fileName,
        String fileContentType,
        long fileSize) {
}
