package com.legalease.service.impl;

import com.legalease.service.DocumentParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class TikaDocumentParserServiceImpl implements DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(TikaDocumentParserServiceImpl.class);

    private final Tika tika = new Tika();

    @Override
    public String parseDocument(MultipartFile file) throws IOException {
        log.info("Parsing document file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
        try (InputStream inputStream = file.getInputStream()) {
            String content = tika.parseToString(inputStream);
            log.info("Successfully parsed document. Character length: {}", content.length());
            return content;
        } catch (Exception e) {
            log.error("Error occurred while parsing file using Tika", e);
            throw new IOException("Failed to parse document text", e);
        }
    }
}
