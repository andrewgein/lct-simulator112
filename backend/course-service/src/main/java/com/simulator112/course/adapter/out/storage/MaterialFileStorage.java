package com.simulator112.course.adapter.out.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class MaterialFileStorage {
    private static final Set<String> EXTENSIONS = Set.of("md", "pdf", "doc", "docx", "xls", "xlsx");
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "md", "text/markdown",
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final S3Client s3;

    @Value("${storage.s3.bucket:course-materials}")
    private String bucket;
    private final AtomicBoolean bucketReady = new AtomicBoolean();

    private void ensureBucket() {
        if (bucketReady.get()) return;
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) throw exception;
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
        bucketReady.set(true);
    }

    public StoredMaterialFile upload(UUID userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Нельзя загрузить пустой файл");
        return upload(userId, file.getOriginalFilename(), file.getBytes());
    }

    public StoredMaterialFile upload(UUID userId, String requestedName, byte[] bytes) {
        ensureBucket();
        String originalName = sanitizeName(requestedName);
        String extension = extension(originalName);
        if (!EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Поддерживаются файлы MD, PDF, DOC, DOCX, XLS и XLSX");
        }
        if (bytes.length == 0) throw new IllegalArgumentException("Нельзя загрузить пустой файл");
        String objectKey = "materials/" + userId + "/" + UUID.randomUUID() + "." + extension;
        String contentType = CONTENT_TYPES.get(extension);
        String encodedOriginalName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(originalName.getBytes(StandardCharsets.UTF_8));
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(objectKey).contentType(contentType)
                        .metadata(Map.of("original-name-base64url", encodedOriginalName)).build(),
                RequestBody.fromBytes(bytes));
        return new StoredMaterialFile(objectKey, originalName, contentType, bytes.length);
    }

    public StoredObject download(String objectKey) {
        ensureBucket();
        ResponseBytes<GetObjectResponse> response = s3.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(objectKey).build());
        return new StoredObject(response.asByteArray(), response.response().contentType());
    }

    public void requireOwnedBy(String objectKey, UUID userId) {
        if (objectKey == null || !objectKey.startsWith("materials/" + userId + "/")) {
            throw new IllegalArgumentException("Некорректный ключ файла материала");
        }
    }

    private String sanitizeName(String name) {
        if (name == null || name.isBlank()) return "document";
        String normalized = name.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "_");
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredMaterialFile(String objectKey, String fileName, String contentType, long size) {}
    public record StoredObject(byte[] bytes, String contentType) {}
}
