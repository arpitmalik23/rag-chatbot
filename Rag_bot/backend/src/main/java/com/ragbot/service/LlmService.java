package com.ragbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ragbot.model.SourceChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Builds a context-grounded prompt from retrieved chunks and calls
 * Groq's OpenAI-compatible chat completions endpoint to produce the
 * final answer.
 *
 * Groq only serves open-weight chat models (Llama, Qwen, etc.) — it does
 * not offer an embeddings endpoint, so embeddings still go through
 * Gemini via EmbeddingService. Only answer generation is on Groq.
 *
 * Docs: https://console.groq.com/docs/openai
 */
@Service
public class LlmService {

    private final WebClient groqWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.chat-model:llama-3.3-70b-versatile}")
    private String chatModel;

    public LlmService(WebClient groqWebClient) {
        this.groqWebClient = groqWebClient;
    }

    public String generateAnswer(String question, List<SourceChunk> contextChunks) {
        String systemPrompt = """
                You are a helpful assistant answering questions about a document.
                Use ONLY the excerpts the user provides to answer. If the answer
                isn't in the excerpts, say you don't have enough information in
                the document. Keep the answer concise and cite excerpt numbers
                where relevant.
                """;

        String userPrompt = buildUserPrompt(question, contextChunks);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", chatModel);
        body.put("temperature", 0.2);
        body.put("max_tokens", 1024);

        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        body.set("messages", messages);

        JsonNode response = groqWebClient.post()
                .uri("/chat/completions")
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return extractText(response);
    }

    private String buildUserPrompt(String question, List<SourceChunk> contextChunks) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < contextChunks.size(); i++) {
            SourceChunk chunk = contextChunks.get(i);
            context.append("[Excerpt ").append(i + 1)
                    .append(", page ").append(chunk.getPage()).append("]\n")
                    .append(chunk.getText()).append("\n\n");
        }

        return """
                Document excerpts:
                %s

                Question: %s
                """.formatted(context.toString(), question);
    }

    private String extractText(JsonNode response) {
        if (response == null) return "";
        JsonNode content = response
                .path("choices").path(0)
                .path("message").path("content");
        return content.isMissingNode() ? "" : content.asText();
    }
}
