package com.legalease.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface DocumentParserService {
    /**
     * Parses document file and extracts text.
     * @param file PDF or DOCX file.
     * @return Extracted plain text content.
     */
    String parseDocument(MultipartFile file) throws IOException;
}
