package com.simulator112.review_service.adapter.out.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class CallRecordingS3Config {
    @Bean
    S3Client callRecordingS3Client(
            @Value("${review.recordings.s3.endpoint}") URI endpoint,
            @Value("${review.recordings.s3.region}") String region,
            @Value("${review.recordings.s3.access-key}") String accessKey,
            @Value("${review.recordings.s3.secret-key}") String secretKey) {
        return S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
