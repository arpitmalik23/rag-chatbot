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
 * Gemini's generateContent endpoint to produce the final answer.
 */
@Service
public class LlmService {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.chat-model:gemini-1.5-flash}")
    private String chatModel;

    public LlmService(WebClient geminiWebClient) {
        this.geminiWebClient = geminiWebClient;
    }

    public String generateAnswer(String question, List<SourceChunk> contextChunks) {
        String prompt = buildPrompt(question, contextChunks);

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = objectMapper.createArrayNode();
        ObjectNode content = objectMapper.createObjectNode();
        ArrayNode parts = objectMapper.createArrayNode();
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", prompt);
        parts.add(part);
        content.set("parts", parts);
        contents.add(content);
        body.set("contents", contents);

        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", 1024);
        body.set("generationConfig", generationConfig);

        String path = String.format("/models/%s:generateContent?key=%s", chatModel, apiKey);

        JsonNode response = geminiWebClient.post()
                .uri(path)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return extractText(response);
    }

    private String buildPrompt(String question, List<SourceChunk> contextChunks) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < contextChunks.size(); i++) {
            SourceChunk chunk = contextChunks.get(i);
            context.append("[Excerpt ").append(i + 1)
                    .append(", page ").append(chunk.getPage()).append("]\n")
                    .append(chunk.getText()).append("\n\n");
        }

        return """
                You are a helpful assistant answering questions about a document.
                Use ONLY the excerpts below to answer. If the answer isn't in the
                excerpts, say you don't have enough information in the document.
                Keep the answer concise and cite excerpt numbers where relevant.

                Document excerpts:
                %s

                Question: %s

                Answer:
                """.formatted(context.toString(), question);
    }

    private String extractText(JsonNode response) {
        if (response == null) return "";
        JsonNode textNode = response
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text");
        return textNode.isMissingNode() ? "" : textNode.asText();
    }
}
