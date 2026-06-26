package com.legalease.service;

import com.legalease.entity.Template;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TemplateService {
    List<Template> getAllTemplates();
    Template getTemplateBySlug(String slug);
    String generateContract(String slug, Map<String, String> values);
    byte[] exportToPdf(String title, String filledText) throws IOException;
}
