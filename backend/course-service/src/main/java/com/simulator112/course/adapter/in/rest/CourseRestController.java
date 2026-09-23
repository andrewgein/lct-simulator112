package com.simulator112.course.adapter.in.rest;

import com.simulator112.course.adapter.in.rest.dto.CourseRequest;
import com.simulator112.course.adapter.in.rest.dto.CourseView;
import com.simulator112.course.adapter.in.rest.dto.MaterialFileUploadView;
import com.simulator112.course.adapter.out.persistence.repository.SpringDataCourseMaterialRepository;
import com.simulator112.course.adapter.out.storage.MaterialFileStorage;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import com.simulator112.course.application.port.in.CreateCourseUseCase;
import com.simulator112.course.application.port.in.FindAuthoredCoursesUseCase;
import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.UpdateCourseUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseRestController {
    private final CreateCourseUseCase createCourse;
    private final UpdateCourseUseCase updateCourse;
    private final GetCourseUseCase getCourse;
    private final FindAuthoredCoursesUseCase findAuthoredCourses;
    private final CourseRestMapper mapper;
    private final MaterialFileStorage fileStorage;
    private final SpringDataCourseMaterialRepository materialRepository;
    private final EnrollmentRepository enrollmentRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseView create(@RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody CourseRequest request) {
        return mapper.toView(createCourse.createCourse(mapper.toDomain(null, request, userId)));
    }

    @PutMapping("/{courseId}")
    public CourseView update(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID courseId,
                             @Valid @RequestBody CourseRequest request) {
        return mapper.toView(updateCourse.updateCourse(courseId, mapper.toDomain(courseId, request, userId), userId));
    }

    @GetMapping("/{courseId}")
    public CourseView get(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID courseId) {
        var course = getCourse.getCourse(courseId);
        if (!course.authorId().equals(userId)
                && enrollmentRepository.findByCourseIdAndStudentId(courseId, userId).isEmpty()) {
            throw new CourseAccessDeniedException("Курс не назначен пользователю");
        }
        return mapper.toView(course);
    }

    @GetMapping
    public List<CourseView> findAuthored(@RequestHeader("X-User-Id") UUID userId) {
        return findAuthoredCourses.findAuthoredCourses(userId).stream().map(mapper::toView).toList();
    }

    @PostMapping(path = "/material-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialFileUploadView uploadMaterialFile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestPart("file") MultipartFile file) throws IOException {
        var stored = fileStorage.upload(userId, file);
        return new MaterialFileUploadView(stored.objectKey(), stored.fileName(), stored.contentType(), stored.size());
    }

    @GetMapping("/materials/{materialId}/file")
    public ResponseEntity<byte[]> downloadMaterialFile(@RequestHeader("X-User-Id") UUID userId,
                                                        @PathVariable UUID materialId) {
        var material = materialRepository.findWithCourseById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Материал не найден: " + materialId));
        var course = material.getCourse();
        if (!course.getAuthorId().equals(userId)
                && enrollmentRepository.findByCourseIdAndStudentId(course.getId(), userId).isEmpty()) {
            throw new CourseAccessDeniedException("Материал доступен только автору и назначенным ученикам");
        }
        if (material.getFileObjectKey() == null) {
            throw new IllegalArgumentException("У материала нет файла");
        }
        var stored = fileStorage.download(material.getFileObjectKey());
        MediaType contentType = MediaType.parseMediaType(
                material.getFileContentType() == null ? stored.contentType() : material.getFileContentType());
        boolean inline = contentType.isCompatibleWith(MediaType.TEXT_MARKDOWN)
                || contentType.isCompatibleWith(MediaType.APPLICATION_PDF);
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(material.getFileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(stored.bytes().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(stored.bytes());
    }
}
