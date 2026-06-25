package com.legalease.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Value("${app.supabase.s3-endpoint}")
    private String s3Endpoint;

    @Value("${app.supabase.s3-region}")
    private String s3Region;

    @Value("${app.supabase.access-key}")
    private String accessKey;

    @Value("${app.supabase.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        // Build AWS S3 client pointing to Supabase S3-compatible endpoint
        return S3Client.builder()
                .endpointOverride(URI.create(s3Endpoint))
                .region(Region.of(s3Region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // Crucial for non-AWS S3 compatibility
                        .build())
                .build();
    }
}
