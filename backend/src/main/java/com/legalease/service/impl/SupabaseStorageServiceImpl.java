package com.legalease.service.impl;

import com.legalease.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
public class SupabaseStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageServiceImpl.class);

    private final S3Client s3Client;

    public SupabaseStorageServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Value("${app.supabase.bucket-name}")
    private String bucketName;

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Override
    public String uploadFile(MultipartFile file, String fileName) throws IOException {
        log.info("Starting upload of file: {} to bucket: {}", fileName, bucketName);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Successfully uploaded file: {} to Supabase Storage", fileName);

            // Construct public URL. Format: https://<project-id>.supabase.co/storage/v1/object/public/<bucket>/<key>
            return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, fileName);
        } catch (Exception e) {
            log.error("Failed to upload file to Supabase S3 compatibility storage", e);
            throw new IOException("Storage upload failed", e);
        }
    }
}
