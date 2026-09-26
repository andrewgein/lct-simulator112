package com.simulator112.review_service.adapter.out.storage;

import com.simulator112.review_service.application.port.out.CallRecordingStore;
import com.simulator112.review_service.domain.model.CallRecording;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@RequiredArgsConstructor
public class CallRecordingS3Adapter implements CallRecordingStore {
    private static final Pattern SAFE_PART = Pattern.compile("[a-zA-Z0-9_.-]+");
    private static final Pattern FILE_NAME = Pattern.compile("\\d{8}T\\d{6}_\\d{6}Z\\.wav");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss_SSSSSS'Z'").withZone(ZoneOffset.UTC);

    private final S3Client callRecordingS3Client;

    @Value("${review.recordings.s3.bucket}")
    private String bucket;

    @Override
    public List<CallRecording> findByContextId(UUID contextId) {
        String prefix = "recordings/" + contextId + "/";
        try {
            return callRecordingS3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
                            .bucket(bucket).prefix(prefix).build()).contents().stream()
                    .map(item -> fromKey(prefix, item.key(), item.lastModified()))
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(CallRecording::startedAt))
                    .toList();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) return List.of();
            throw exception;
        }
    }

    @Override
    public Optional<byte[]> findContent(UUID contextId, String callId, String fileName) {
        if (!SAFE_PART.matcher(callId).matches() || !FILE_NAME.matcher(fileName).matches()) {
            return Optional.empty();
        }
        String key = "recordings/" + contextId + "/" + callId + "/" + fileName;
        try {
            ResponseBytes<GetObjectResponse> object = callRecordingS3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return Optional.of(object.asByteArray());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) return Optional.empty();
            throw exception;
        }
    }

    private Optional<CallRecording> fromKey(String prefix, String key, Instant fallbackTime) {
        String relative = key.substring(prefix.length());
        String[] parts = relative.split("/", -1);
        if (parts.length != 2 || !SAFE_PART.matcher(parts[0]).matches()
                || !FILE_NAME.matcher(parts[1]).matches()) {
            return Optional.empty();
        }
        try {
            String timestamp = parts[1].substring(0, parts[1].length() - 4);
            return Optional.of(new CallRecording(parts[0], parts[1], FILE_TIME.parse(timestamp, Instant::from)));
        } catch (DateTimeParseException exception) {
            return Optional.of(new CallRecording(parts[0], parts[1], fallbackTime));
        }
    }
}
