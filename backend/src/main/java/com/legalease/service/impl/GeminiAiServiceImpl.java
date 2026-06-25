package com.legalease.service.impl;

import com.legalease.service.AiService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiAiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiServiceImpl.class);

    @Value("${app.gemini.api-key}")
    private String apiKey;

    private ChatLanguageModel model;

    @PostConstruct
    public void init() {
        log.info("Initializing Gemini Chat Model...");
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.0-flash")
                .build();
    }

    @Override
    public String generateSummary(String redactedText) {
        log.info("Generating structured legal analysis with Gemini for text of length: {}", redactedText.length());

        String systemPrompt = """
                You are an expert legal AI assistant specializing in Nepal law.
                Analyze the following legal document (which has been PII-redacted) and return a highly accurate, structured analysis in JSON format.
                
                The JSON response MUST exactly match the following schema:
                {
                  "title": "Clear concise document title",
                  "summary": "An easy-to-read, plain language summary of the document, explaining it simply in both English and Nepali. Highlight the core purpose and obligations.",
                  "riskLevel": "LOW | MEDIUM | HIGH",
                  "keyClauses": [
                    {
                      "text": "Exact quote of the key clause from the text",
                      "risk": "SAFE | REVIEW | DANGER",
                      "explanation": "Explain what this means in simple language. If dangerous or review-worthy, explain why and cite any relevant Nepal Law if applicable."
                    }
                  ]
                }
                
                Guidelines:
                1. If a clause is highly unfair or potentially illegal in Nepal (e.g. eviction with 3 days notice), flag it as DANGER and explain why under Nepal law context.
                2. Respond with ONLY the raw JSON output. Do not wrap it in markdown code blocks like ```json ... ```. Just return the JSON object directly.
                """;

        String userMessage = "Here is the document text to analyze:\n\n" + redactedText;

        try {
            String response = model.generate(userMessage + "\n\nInstructions:\n" + systemPrompt);
            log.debug("Gemini raw response length: {}", response.length());
            
            // Clean markdown wrap if Gemini adds it despite instructions
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            
            return cleanedResponse.trim();
        } catch (Exception e) {
            log.error("Failed to generate summary with Gemini model", e);
            throw new RuntimeException("Gemini generation failed", e);
        }
    }
}
