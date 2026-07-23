package com.ragbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps calls to the Gemini embedding endpoint
 * (models/gemini-embedding-001:embedContent).
 *
 * Note: text-embedding-004 was retired by Google — gemini-embedding-001
 * is the current replacement. It defaults to 3072-dim output, so we
 * explicitly request outputDimensionality to keep vectors the same size
 * the Qdrant collection was created with (see gemini.embedding-dimensions).
 *
 * Docs: https://ai.google.dev/gemini-api/docs/embeddings
 */
@Service
public class EmbeddingService {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.embedding-model:gemini-embedding-001}")
    private String embeddingModel;

    @Value("${gemini.embedding-dimensions:768}")
    private int embeddingDimensions;

    public EmbeddingService(WebClient geminiWebClient) {
        this.geminiWebClient = geminiWebClient;
    }

    /**
     * Embeds a single string (used for the user's question at query time).
     */
    public List<Float> embed(String text) {
        String path = String.format("/models/%s:embedContent?key=%s", embeddingModel, apiKey);

        String body = """
                {
                  "model": "models/%s",
                  "content": { "parts": [ { "text": %s } ] },
                  "outputDimensionality": %d
                }
                """.formatted(embeddingModel, toJsonString(text), embeddingDimensions);

        JsonNode response = geminiWebClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return parseEmbedding(response);
    }

    /**
     * Embeds a batch of chunk texts sequentially. Gemini's batchEmbedContents
     * endpoint could replace this loop for higher throughput on large PDFs.
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        List<List<Float>> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    private List<Float> parseEmbedding(JsonNode response) {
        List<Float> values = new ArrayList<>();
        if (response == null) return values;
        JsonNode valuesNode = response.path("embedding").path("values");
        if (valuesNode.isArray()) {
            for (JsonNode v : valuesNode) {
                values.add((float) v.asDouble());
            }
        }
        return values;
    }

    private String toJsonString(String raw) {
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize text for embedding request", e);
        }
    }
}
