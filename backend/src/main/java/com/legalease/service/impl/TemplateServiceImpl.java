package com.legalease.service.impl;

import com.legalease.entity.Template;
import com.legalease.repository.TemplateRepository;
import com.legalease.service.TemplateService;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class TemplateServiceImpl implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private final TemplateRepository templateRepository;

    public TemplateServiceImpl(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public List<Template> getAllTemplates() {
        return templateRepository.findByIsPublicTrue();
    }

    @Override
    public Template getTemplateBySlug(String slug) {
        return templateRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with slug: " + slug));
    }

    @Override
    public String generateContract(String slug, Map<String, String> values) {
        Template template = getTemplateBySlug(slug);
        String filledContent = template.getContent();
        
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (val == null || val.trim().isEmpty()) {
                val = "____________________";
            }
            filledContent = filledContent.replace("{{" + key + "}}", val);
        }
        
        // Clean up any unfilled variables
        filledContent = filledContent.replaceAll("\\{\\{[^}]+\\}\\}", "____________________");
        return filledContent;
    }

    @Override
    public byte[] exportToPdf(String title, String filledText) throws IOException {
        log.info("Generating PDF for title: {}", title);
        
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            // Try to load a unicode font supporting Devanagari, fallback to Helvetica
            BaseFont baseFont = getDevanagariBaseFont();
            com.lowagie.text.Font fontTitle = new com.lowagie.text.Font(baseFont, 16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font fontBody = new com.lowagie.text.Font(baseFont, 12, com.lowagie.text.Font.NORMAL);
            
            // Add Title
            Paragraph pTitle = new Paragraph(title, fontTitle);
            pTitle.setAlignment(Paragraph.ALIGN_CENTER);
            pTitle.setSpacingAfter(20);
            document.add(pTitle);
            
            // Add Content
            String[] paragraphs = filledText.split("\n");
            for (String para : paragraphs) {
                if (!para.trim().isEmpty()) {
                    Paragraph p = new Paragraph(para, fontBody);
                    p.setSpacingAfter(10);
                    p.setLeading(16f); // Good line height
                    document.add(p);
                } else {
                    document.add(new Paragraph(" ", fontBody));
                }
            }
            
            document.close();
        } catch (Exception de) {
            log.error("Failed to generate PDF document", de);
            throw new IOException("PDF generation failed: " + de.getMessage(), de);
        }
        
        return out.toByteArray();
    }

    private BaseFont getDevanagariBaseFont() {
        String[] fontPaths = {
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/System/Library/Fonts/Supplemental/NotoSansDevanagari-Regular.ttf",
            "/Library/Fonts/Arial Unicode.ttf",
            "C:\\Windows\\Fonts\\mangal.ttf",
            "C:\\Windows\\Fonts\\arialuni.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
        };
        
        for (String path : fontPaths) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    log.info("Loading Devanagari system font from path: {}", path);
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception e) {
                // Ignore and try next path
            }
        }
        
        try {
            log.warn("Unicode font not found. Falling back to default HELVETICA (Devanagari text might render incorrectly).");
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Could not create default PDF font", e);
        }
    }
}
