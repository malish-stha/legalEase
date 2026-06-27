package com.legalease.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    /**
     * Uploads file to Supabase storage bucket.
     * @param file The file to upload.
     * @param fileName Unique file name or path inside bucket.
     * @return Public URL of the uploaded file.
     */
    String uploadFile(MultipartFile file, String fileName) throws IOException;

    /**
     * Deletes file from Supabase storage bucket.
     * @param fileUrl The public URL of the file to delete.
     */
    void deleteFile(String fileUrl);
}
