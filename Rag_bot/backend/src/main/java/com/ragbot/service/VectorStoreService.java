package com.ragbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ragbot.model.DocChunk;
import com.ragbot.model.SourceChunk;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around Qdrant's REST API. Every document chunk is stored
 * as a point with the session id and doc id in its payload so searches
 * can be scoped to the current chat session.
 */
@Service
public class VectorStoreService {

    private final WebClient qdrantWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${qdrant.collection:doc_chunks}")
    private String collectionName;

    @Value("${gemini.embedding-dimensions:768}")
    private int vectorSize;

    public VectorStoreService(WebClient qdrantWebClient) {
        this.qdrantWebClient = qdrantWebClient;
    }

    /**
     * Ensures the collection exists on startup. Safe to call repeatedly —
     * Qdrant returns a 4xx that we swallow if it's already there.
     */
    @PostConstruct
    public void ensureCollection() {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode vectors = objectMapper.createObjectNode();
        vectors.put("size", vectorSize);
        vectors.put("distance", "Cosine");
        body.set("vectors", vectors);

        try {
            qdrantWebClient.put()
                    .uri("/collections/" + collectionName)
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            // Collection likely already exists — fine to ignore on subsequent boots.
        }

        ensureSessionIdIndex();
    }

    /**
     * Qdrant (especially Qdrant Cloud) requires an explicit payload index
     * before a field can be used in a search filter. Without this, searches
     * filtered by sessionId fail with a 400 "Index required but not found".
     * Safe to call repeatedly — Qdrant no-ops if the index already exists.
     */
    private void ensureSessionIdIndex() {
        ObjectNode indexBody = objectMapper.createObjectNode();
        indexBody.put("field_name", "sessionId");
        indexBody.put("field_schema", "keyword");

        try {
            qdrantWebClient.put()
                    .uri("/collections/" + collectionName + "/index")
                    .bodyValue(indexBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            // Index likely already exists — fine to ignore on subsequent boots.
        }
    }

    /**
     * Upserts a batch of chunks (with their pre-computed embeddings) as
     * Qdrant points.
     */
    public void upsert(List<DocChunk> chunks) {
        ArrayNode points = objectMapper.createArrayNode();

        for (DocChunk chunk : chunks) {
            ObjectNode point = objectMapper.createObjectNode();
            point.put("id", chunk.getId());

            ArrayNode vector = objectMapper.createArrayNode();
            chunk.getEmbedding().forEach(vector::add);
            point.set("vector", vector);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("text", chunk.getText());
            payload.put("page", chunk.getPage());
            payload.put("docId", chunk.getDocId());
            payload.put("sessionId", chunk.getSessionId());
            payload.put("chunkIndex", chunk.getChunkIndex());
            point.set("payload", payload);

            points.add(point);
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.set("points", points);

        qdrantWebClient.put()
                .uri("/collections/" + collectionName + "/points?wait=true")
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
    /**
    * Deletes all previously stored chunks for a session, so a new upload
    * replaces the old document instead of being searched alongside it.
    */
    public void deleteBySession(String sessionId) {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode filter = objectMapper.createObjectNode();
        ArrayNode must = objectMapper.createArrayNode();
        ObjectNode sessionMatch = objectMapper.createObjectNode();
        sessionMatch.put("key", "sessionId");
        ObjectNode matchValue = objectMapper.createObjectNode();
        matchValue.put("value", sessionId);
        sessionMatch.set("match", matchValue);
        must.add(sessionMatch);
        filter.set("must", must);
        body.set("filter", filter);

        qdrantWebClient.post()
                .uri("/collections/" + collectionName + "/points/delete")
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
    /**
     * Searches for the top-k chunks closest to the query embedding,
     * scoped to the given session so one user's documents don't leak
     * into another's answers.
     */
    public List<SourceChunk> search(List<Float> queryEmbedding, String sessionId, int topK) {
        ObjectNode body = objectMapper.createObjectNode();

        ArrayNode vector = objectMapper.createArrayNode();
        queryEmbedding.forEach(vector::add);
        body.set("vector", vector);

        body.put("limit", topK);
        body.put("with_payload", true);

        ObjectNode filter = objectMapper.createObjectNode();
        ArrayNode must = objectMapper.createArrayNode();
        ObjectNode sessionMatch = objectMapper.createObjectNode();
        sessionMatch.put("key", "sessionId");
        ObjectNode matchValue = objectMapper.createObjectNode();
        matchValue.put("value", sessionId);
        sessionMatch.set("match", matchValue);
        must.add(sessionMatch);
        filter.set("must", must);
        body.set("filter", filter);

        JsonNode response = qdrantWebClient.post()
                .uri("/collections/" + collectionName + "/points/search")
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return parseResults(response);
    }

    private List<SourceChunk> parseResults(JsonNode response) {
        List<SourceChunk> results = new ArrayList<>();
        if (response == null) return results;

        JsonNode result = response.path("result");
        if (result.isArray()) {
            for (JsonNode hit : result) {
                String text = hit.path("payload").path("text").asText("");
                int page = hit.path("payload").path("page").asInt(0);
                double score = hit.path("score").asDouble(0.0);
                results.add(new SourceChunk(text, page, score));
            }
        }
        return results;
    }
}
