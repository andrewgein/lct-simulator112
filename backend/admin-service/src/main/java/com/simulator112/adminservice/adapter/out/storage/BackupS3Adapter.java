package com.simulator112.adminservice.adapter.out.storage;

import com.simulator112.adminservice.application.port.out.BackupObjectStorage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
@RequiredArgsConstructor
public class BackupS3Adapter implements BackupObjectStorage {

  private final S3Client s3Client;

  @Value("${s3.backup-bucket}")
  private String bucket;

  @Value("${backup.retention-days}")
  private int retentionDays;

  @Override
  public void ensureBucketReady() {
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
    } catch (NoSuchBucketException e) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    }
  }

  @Override
  public long upload(Path file, String key) throws IOException {
    long size = Files.size(file);
    s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromFile(file));
    return size;
  }

  @Override
  public List<String> listKeys(String prefix) {
    return s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build())
        .contents()
        .stream()
        .map(S3Object::key)
        .toList();
  }

  @Override
  public InputStream open(String key) {
    return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
  }

  @Override
  public void applyRetentionPolicy() {
    Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
    var listing = s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build());
    for (S3Object object : listing.contents()) {
      if (object.lastModified().isBefore(cutoff)) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(object.key()).build());
      }
    }
  }
}
