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
                .modelName("gemini-2.5-flash")
                .build();

        this.embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-004")
                .build();

        this.streamingModel = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
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

    @Override
    public String checkLaborCompliance(String documentText) {
        log.info("Running Nepal Labor Act compliance checks for text length: {}", documentText.length());

        String systemPrompt = """
                You are an expert legal AI compliance officer specializing in Nepal Labor Law (Labor Act, 2074 / श्रम ऐन, २०७४).
                Analyze the provided employment contract text and check it clause-by-clause against the following key provisions of the Nepal Labor Act 2074:
                1. Probation Period (Section 13): Maximum of 6 months. Any longer period is non-compliant.
                2. Working Hours (Section 28): Standard working hours are maximum 8 hours per day and 48 hours per week.
                3. Overtime Rate (Section 30): Overtime pay must be at least 1.5 times the regular remuneration. Maximum overtime allowed is 24 hours per week.
                4. Weekly Holiday (Section 40): At least 1 day paid weekly holiday is mandatory.
                5. Public Holidays (Section 41): Employees are entitled to at least 13 days of paid public holidays annually (14 days for females).
                6. Annual (Home) Leave (Section 42): 1 day for every 20 days worked.
                7. Sick Leave (Section 43): At least 12 days fully paid sick leave annually.
                8. Mourning Leave (Section 44): 13 days of paid mourning leave in the event of the death of a close family member.
                9. Maternity/Paternity Leave (Section 45): Maternity leave must be at least 98 days total (60 days fully paid). Paternity leave must be at least 15 days fully paid.
                10. Termination Notice (Section 144): At least 1 day notice for tenure up to 4 weeks; at least 7 days for more than 4 weeks to 1 year; at least 30 days for tenure exceeding 1 year.
                
                For each provision, determine if the contract is COMPLIANT, NON_COMPLIANT, or NOT_SPECIFIED in the contract text.
                Return your analysis in JSON format matching this EXACT schema:
                [
                  {
                    "provision": "Name of the provision (e.g. Probation Period)",
                    "status": "COMPLIANT | NON_COMPLIANT | NOT_SPECIFIED",
                    "clauseText": "Quote the exact wording of the clause in the contract addressing this, or write 'Not found' if not specified.",
                    "lawReference": "Cite the relevant section of the Nepal Labor Act 2074 (e.g. Section 13)",
                    "explanation": "Brief explanation of why it is compliant or non-compliant, explaining it simply in both English and Nepali. State the minimum requirements if non-compliant."
                  }
                ]
                
                Respond with ONLY the raw JSON output. Do not wrap it in markdown code blocks like ```json ... ```. Just return the JSON array directly.
                """;

        String userMessage = "Here is the contract text to evaluate:\n\n" + documentText;

        try {
            String response = model.generate(userMessage + "\n\nInstructions:\n" + systemPrompt);
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            return cleanedResponse.trim();
        } catch (Exception e) {
            log.error("Failed to run Labor Law compliance check via Gemini model", e);
            throw new RuntimeException("Gemini compliance check failed", e);
        }
    }

    @Override
    public String compareDocuments(String baseText, String compareText) {
        log.info("Comparing two documents. Base text size: {}, Compare text size: {}", baseText.length(), compareText.length());

        String systemPrompt = """
                You are an expert legal AI advisor specializing in document comparison.
                Compare the original (base) contract text with the modified (comparison) contract text.
                Identify all differences, classifying each change as:
                - ADDED: A new clause or sentence was added.
                - DELETED: A clause or sentence was deleted.
                - MODIFIED: A clause was edited (e.g. rent changed from 15k to 20k).
                - SUSPICIOUS: A potentially dangerous change (e.g. hiding an eviction term, removing liability clauses, changing jurisdiction secretly).
                
                Evaluate the modifications carefully and explain what they mean for both parties.
                
                Return the comparison result in JSON format matching this EXACT schema:
                {
                  "overallSummary": "A concise summary of what has changed between the two versions and the overall impact.",
                  "changes": [
                    {
                      "type": "ADDED | DELETED | MODIFIED | SUSPICIOUS",
                      "clauseTitle": "Title or reference for this clause (e.g., Rent, Clause 7, Notice Period)",
                      "originalText": "The text in the base document, or 'Not present' if newly added.",
                      "modifiedText": "The text in the comparison document, or 'Removed' if deleted.",
                      "explanation": "Explain what this change means in simple language in both English and Nepali. Highlight any risk if suspicious."
                    }
                  ]
                }
                
                Respond with ONLY the raw JSON output. Do not wrap it in markdown code blocks like ```json ... ```. Just return the JSON object directly.
                """;

        String userMessage = "Original (Base) Document:\n" + baseText + "\n\n====================\n\nModified (Comparison) Document:\n" + compareText;

        try {
            String response = model.generate(userMessage + "\n\nInstructions:\n" + systemPrompt);
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            return cleanedResponse.trim();
        } catch (Exception e) {
            log.error("Failed to compare documents via Gemini model", e);
            throw new RuntimeException("Gemini document comparison failed", e);
        }
    }

    @Override
    public String preScreenCase(String issueDescription, String documentText) {
        log.info("Running AI pre-screening assessment. Issue length: {}, Doc length: {}", 
                issueDescription != null ? issueDescription.length() : 0, 
                documentText != null ? documentText.length() : 0);

        String systemPrompt = """
                You are a senior legal pre-screening officer specializing in Nepal law.
                Analyze the user's issue description and any provided legal document context.
                Evaluate:
                1. The complexity of the case (LOW, MEDIUM, HIGH).
                2. Potential legal exposure, liabilities, or risks for the user under Nepal Law.
                3. Actionable next steps or recommendations before consulting a lawyer.
                
                Return your analysis in JSON format matching this EXACT schema:
                {
                  "complexityRating": "LOW | MEDIUM | HIGH",
                  "report": "A detailed, structured markdown text summarizing your exposure assessment, relevant Nepal laws (e.g. Civil Code, Labour Act), risks, and advice in both English and Nepali."
                }
                
                Respond with ONLY the raw JSON output. Do not wrap it in markdown code blocks like ```json ... ```. Just return the JSON object directly.
                """;

        String userMessage = "User's Explanation of the Issue:\n" + (issueDescription != null ? issueDescription : "Not provided") + 
                "\\n\\nAssociated Document Context:\\n" + (documentText != null ? documentText : "Not provided");

        try {
            String response = model.generate(userMessage + "\\n\\nInstructions:\\n" + systemPrompt);
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            return cleanedResponse.trim();
        } catch (Exception e) {
            log.error("Failed to pre-screen case via Gemini model", e);
            throw new RuntimeException("Gemini case pre-screening failed", e);
        }
    }
}
