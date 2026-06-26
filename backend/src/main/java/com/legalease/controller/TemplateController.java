package com.legalease.controller;

import com.legalease.entity.Template;
import com.legalease.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<List<Template>> getAllTemplates() {
        log.info("Fetching all public templates");
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Template> getTemplateBySlug(@PathVariable("slug") String slug) {
        log.info("Fetching template for slug: {}", slug);
        return ResponseEntity.ok(templateService.getTemplateBySlug(slug));
    }

    @PostMapping("/{slug}/generate")
    public ResponseEntity<Map<String, String>> generateContract(
            @PathVariable("slug") String slug,
            @RequestBody Map<String, String> values) {
        log.info("Generating contract for template slug: {}", slug);
        String filledText = templateService.generateContract(slug, values);
        return ResponseEntity.ok(Map.of("content", filledText));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportToPdf(@RequestBody ExportRequest request) throws IOException {
        log.info("Exporting generated contract to PDF. Title: {}", request.getTitle());
        byte[] pdfBytes = templateService.exportToPdf(request.getTitle(), request.getContent());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    public static class ExportRequest {
        private String title;
        private String content;

        public ExportRequest() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
