package com.legalease.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalease.entity.Conversation;
import com.legalease.entity.Document;
import com.legalease.entity.User;
import com.legalease.repository.ConversationRepository;
import com.legalease.repository.DocEmbeddingRepository;
import com.legalease.repository.DocumentRepository;
import com.legalease.repository.UserRepository;
import com.legalease.service.AiService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocEmbeddingRepository docEmbeddingRepository;
    private final ConversationRepository conversationRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatController(
            UserRepository userRepository,
            DocumentRepository documentRepository,
            DocEmbeddingRepository docEmbeddingRepository,
            ConversationRepository conversationRepository,
            AiService aiService) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.docEmbeddingRepository = docEmbeddingRepository;
        this.conversationRepository = conversationRepository;
        this.aiService = aiService;
    }

    public static class ChatMessage {
        private String role; // user | assistant
        private String content;

        public ChatMessage() {}
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @RequestParam("docId") UUID docId,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Fetching chat history for doc: {} and user: {}", docId, clerkUserId);

        OptionalConversationWrapper wrapper = getOrCreateConversation(clerkUserId, docId);
        if (wrapper == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ChatMessage> history = parseHistory(wrapper.conversation.getMessages());
        return ResponseEntity.ok(history);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam("docId") UUID docId,
            @RequestParam("message") String message,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Initiating streaming chat for doc: {} and query: {}", docId, message);

        SseEmitter emitter = new SseEmitter(120000L); // 2 minute timeout

        OptionalConversationWrapper wrapper = getOrCreateConversation(clerkUserId, docId);
        if (wrapper == null) {
            emitter.completeWithError(new IllegalArgumentException("Invalid user or document"));
            return emitter;
        }

        Conversation conversation = wrapper.conversation;
        Document document = wrapper.document;

        try {
            // 1. Generate text vector for the user query
            float[] queryVector = aiService.embedText(message);
            String vectorStr = Arrays.toString(queryVector);

            // 2. Fetch top 3 matching chunks
            List<String> similarChunks = docEmbeddingRepository.findSimilarChunks(docId, vectorStr, 3);

            // 3. Retrieve and format conversation history
            List<ChatMessage> history = parseHistory(conversation.getMessages());
            StringBuilder historyBuilder = new StringBuilder();
            for (ChatMessage msg : history) {
                historyBuilder.append(msg.getRole().equalsIgnoreCase("user") ? "User: " : "Assistant: ")
                        .append(msg.getContent()).append("\n");
            }

            // 4. Construct System Prompt with context citations
            StringBuilder contextBuilder = new StringBuilder();
            if (!similarChunks.isEmpty()) {
                contextBuilder.append("Relevant Context Chunks from the Document:\n");
                for (int i = 0; i < similarChunks.size(); i++) {
                    contextBuilder.append(String.format("[Citation %d]: \"%s\"\n\n", i + 1, similarChunks.get(i)));
                }
            } else {
                contextBuilder.append("No direct context chunks found. Answer based on general legal principles in Nepal.");
            }

            String systemPrompt = String.format("""
                You are LegalEase AI, a professional legal assistant specializing in Nepalese laws.
                You are helping a user understand their uploaded document. Use the provided document context chunks to answer their questions accurately.

                Always cite your sources using the notation [Citation 1], [Citation 2] if you base your answer on that section.
                If the document does not contain the answer, answer based on general legal regulations in Nepal, but mention that the document itself does not specify this.
                Keep answers clear, highly professional, and simple to understand in both English or Nepali as appropriate.

                CRITICAL DOMAIN GUARDRAILS:
                1. You are strictly restricted to discussing Nepalese law, the uploaded legal document, or legal assistant matters.
                2. Do NOT answer any non-legal questions, including requests to write programming code (like Java, Python, HTML), solve math problems, write generic stories, or discuss topics outside of legal aid.
                3. If the user asks about coding, technical engineering, or any non-legal general topic, you MUST politely refuse, stating exactly: "I am sorry, but as LegalEase AI, I am only authorized to assist with Nepalese legal matters and document-related inquiries. I cannot generate code or address non-legal topics."

                %s
                """, contextBuilder);

            String userPrompt = String.format("""
                %s
                New User Question: %s
                Assistant:
                """, historyBuilder.toString(), message);

            // 5. Trigger streaming model generate
            StringBuilder responseAccumulator = new StringBuilder();
            aiService.streamChatResponse(systemPrompt, userPrompt, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    try {
                        responseAccumulator.append(token);
                        // Send raw token chunk as SSE data event
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception sendEx) {
                        log.error("Failed to send token to SSE emitter", sendEx);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    try {
                        // Append user message and generated model message to history
                        history.add(new ChatMessage("user", message));
                        history.add(new ChatMessage("assistant", responseAccumulator.toString()));
                        
                        conversation.setMessages(objectMapper.writeValueAsString(history));
                        conversationRepository.save(conversation);
                        
                        emitter.send(SseEmitter.event().name("complete").data("[DONE]"));
                        emitter.complete();
                        log.info("Streaming response complete for document chat ID: {}", conversation.getId());
                    } catch (Exception saveEx) {
                        log.error("Failed to serialize or save history at stream completion", saveEx);
                        emitter.completeWithError(saveEx);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Error occurred inside Gemini streaming chat", error);
                    try {
                        emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                    } catch (IOException e) {
                        log.error("Failed to send error state event", e);
                    }
                    emitter.completeWithError(error);
                }
            });

        } catch (Exception e) {
            log.error("Failed to setup chat stream request", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private static class OptionalConversationWrapper {
        Conversation conversation;
        Document document;

        OptionalConversationWrapper(Conversation conversation, Document document) {
            this.conversation = conversation;
            this.document = document;
        }
    }

    private OptionalConversationWrapper getOrCreateConversation(String clerkUserId, UUID docId) {
        Document document = documentRepository.findById(docId).orElse(null);
        if (document == null || !document.getUser().getId().equals(clerkUserId)) {
            log.warn("Invalid document or unauthorized access attempt. docId: {}", docId);
            return null;
        }

        User user = userRepository.findById(clerkUserId).orElse(null);
        if (user == null) {
            log.warn("User profile not synced yet: {}", clerkUserId);
            return null;
        }

        Conversation conversation = conversationRepository.findByUserIdAndDocumentId(clerkUserId, docId)
                .orElseGet(() -> {
                    log.info("Creating new conversation session for user: {} and doc: {}", clerkUserId, docId);
                    Conversation newConv = Conversation.builder()
                            .user(user)
                            .document(document)
                            .messages("[]")
                            .build();
                    return conversationRepository.save(newConv);
                });

        return new OptionalConversationWrapper(conversation, document);
    }

    private List<ChatMessage> parseHistory(String messagesJson) {
        if (messagesJson == null || messagesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(messagesJson, new TypeReference<List<ChatMessage>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse conversation history messages JSON", e);
            return new ArrayList<>();
        }
    }
}
