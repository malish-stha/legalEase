package com.legalease.controller;

import com.legalease.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Controller
@RequestMapping("/api/notifications/whatsapp")
public class WhatsAppBotController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppBotController.class);

    private final AiService aiService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public WhatsAppBotController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(
            value = "/webhook",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    @ResponseBody
    public String handleWhatsAppWebhook(
            @RequestParam("From") String from,
            @RequestParam(value = "Body", required = false) String body,
            @RequestParam(value = "NumMedia", required = false) Integer numMedia,
            @RequestParam(value = "MediaUrl0", required = false) String mediaUrl,
            @RequestParam(value = "MediaContentType0", required = false) String mediaType) {

        log.info("Received WhatsApp webhook from: {}, Body: {}, NumMedia: {}", from, body, numMedia);

        String responseText = "";

        // 1. Handle Photo/Media document uploads
        if (numMedia != null && numMedia > 0 && mediaUrl != null) {
            log.info("Downloading media from Twilio URL: {}", mediaUrl);
            byte[] imageBytes = downloadMedia(mediaUrl);
            if (imageBytes != null) {
                try {
                    String contentType = (mediaType != null) ? mediaType : "image/jpeg";
                    responseText = "📄 *LegalEase Nepal OCR parser triggered...*\n\n";

                    // Run Gemini OCR
                    String extractedText = aiService.performOcr(imageBytes, contentType);
                    log.info("OCR successfully extracted text of length: {}", extractedText.length());

                    // Analyze and summarize the extracted text
                    String analysisJson = aiService.generateSummary(extractedText);
                    
                    // Format summary to user
                    responseText += "✓ Text successfully extracted!\n\n*AI Legal Summary:*\n" + formatSummaryText(analysisJson);
                } catch (Exception ocrEx) {
                    log.error("Failed to parse document from WhatsApp photo upload", ocrEx);
                    responseText = "⚠️ Sorry, we failed to process the document image. Please make sure the photo is clear and try again.";
                }
            } else {
                responseText = "⚠️ Failed to download the document image from Twilio servers. Please try again.";
            }
        } else {
            // 2. Handle text-only legal queries
            if (body == null || body.trim().isEmpty()) {
                responseText = "Namaste! Welcome to *LegalEase Nepal*. Send me a clear photo of any legal document to get an instant AI analysis, or ask me any Nepalese legal questions!";
            } else {
                log.info("Processing legal text query via Gemini: {}", body);
                try {
                    responseText = answerGeneralLegalQuestion(body);
                } catch (Exception chatEx) {
                    log.error("Failed to generate chat response for WhatsApp query", chatEx);
                    responseText = "⚠️ Sorry, we encountered an issue processing your query. Please try again later.";
                }
            }
        }

        // Return XML TwiML response to Twilio
        return String.format("<Response><Message>%s</Message></Response>", escapeXml(responseText));
    }

    private byte[] downloadMedia(String mediaUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mediaUrl))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            }
            log.error("Failed to download media, status: {}", response.statusCode());
        } catch (Exception e) {
            log.error("Error occurred while downloading WhatsApp media file", e);
        }
        return null;
    }

    private String formatSummaryText(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> map = mapper.readValue(json, Map.class);
            String title = (String) map.get("title");
            String summary = (String) map.get("summary");
            String riskLevel = (String) map.get("riskLevel");

            StringBuilder sb = new StringBuilder();
            if (title != null) sb.append("📌 *Title:* ").append(title).append("\n");
            if (summary != null) sb.append("📝 *Summary:* ").append(summary).append("\n");
            if (riskLevel != null) sb.append("🚨 *Risk Profile:* ").append(riskLevel).append("\n");
            return sb.toString();
        } catch (Exception e) {
            if (json != null && json.length() > 300) {
                return json.substring(0, 300) + "...";
            }
            return json;
        }
    }

    private String answerGeneralLegalQuestion(String query) {
        String prompt = String.format("""
            You are LegalEase AI, an assistant specializing strictly in Nepalese law and legal documents.
            Answer the user's question clearly, professionally, and naturally. Keep answers concise as they are sent over WhatsApp.
            
            CRITICAL DOMAIN GUARDRAILS:
            1. You are strictly restricted to Nepalese law, documents, and legal aid matters.
            2. Do NOT answer any non-legal questions, including programming, writing code, general science, math, or creative writing.
            3. If the user asks about coding, technical engineering, or any non-legal general topic, you MUST politely refuse, stating exactly: "I am sorry, but as LegalEase AI, I am only authorized to assist with Nepalese legal matters and document-related inquiries. I cannot generate code or address non-legal topics."

            User Question: %s
            """, query);
        return aiService.generateChatResponse(prompt);
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
