package com.legalease.service.impl;

import com.legalease.service.AiService;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.embedding.Embedding;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Service
public class GeminiAiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiServiceImpl.class);

    @Value("${app.gemini.api-key}")
    private String apiKey;

    private ChatLanguageModel model;
    private EmbeddingModel embeddingModel;
    private StreamingChatLanguageModel streamingModel;

    @PostConstruct
    public void init() {
        log.info("Initializing Gemini Models...");
        
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.0-flash")
                .build();

        this.embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-004")
                .build();

        this.streamingModel = GoogleAiGeminiStreamingChatModel.builder()
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
                1. If a clause is unfair or illegal in Nepal (e.g. eviction with 3 days notice), flag it as DANGER and explain why under Nepal law context.
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

    @Override
    public float[] embedText(String text) {
        log.info("Calculating vector embedding for text length: {}", text.length());
        try {
            Response<Embedding> response = embeddingModel.embed(text);
            return response.content().vector();
        } catch (Exception e) {
            log.error("Failed to calculate embedding", e);
            throw new RuntimeException("Gemini embedding calculation failed", e);
        }
    }

    @Override
    public String performOcr(byte[] imageBytes, String mimeType) {
        log.info("Performing OCR text extraction via Gemini Vision for image size: {} bytes", imageBytes.length);
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            ImageContent imageContent = ImageContent.from(base64Image, mimeType);
            TextContent textContent = TextContent.from("""
                Perform OCR on this image. Extract all text content verbatim, maintaining format where possible. 
                Do not include warnings, explanations, or chats. Output the extracted document text only.
                """);

            UserMessage userMessage = UserMessage.from(textContent, imageContent);
            Response<AiMessage> response = model.generate(userMessage);
            return response.content().text();
        } catch (Exception e) {
            log.error("Failed to perform OCR with Gemini model", e);
            throw new RuntimeException("Gemini Vision OCR extraction failed", e);
        }
    }

    @Override
    public void streamChatResponse(String systemPrompt, String userPrompt, StreamingResponseHandler<AiMessage> handler) {
        log.info("Invoking streaming Gemini response for user query");
        try {
            SystemMessage sysMsg = SystemMessage.from(systemPrompt);
            UserMessage userMsg = UserMessage.from(userPrompt);
            streamingModel.generate(List.of(sysMsg, userMsg), handler);
        } catch (Exception e) {
            log.error("Failed to start Gemini chat stream", e);
            throw new RuntimeException("Streaming setup failed", e);
        }
    }
}
