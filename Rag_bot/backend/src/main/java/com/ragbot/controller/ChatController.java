package com.ragbot.controller;

import com.ragbot.model.ChatRequest;
import com.ragbot.model.ChatResponse;
import com.ragbot.model.ChatTurn;
import com.ragbot.model.SourceChunk;
import com.ragbot.service.EmbeddingService;
import com.ragbot.service.LlmService;
import com.ragbot.service.RedisService;
import com.ragbot.service.VectorStoreService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final LlmService llmService;
    private final RedisService redisService;

    @Value("${rag.top-k:4}")
    private int topK;

    public ChatController(EmbeddingService embeddingService,
                           VectorStoreService vectorStoreService,
                           LlmService llmService,
                           RedisService redisService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.llmService = llmService;
        this.redisService = redisService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {
        try {
            List<Float> questionEmbedding = embeddingService.embed(request.getQuestion());

            List<SourceChunk> topChunks = vectorStoreService.search(
                    questionEmbedding, request.getSessionId(), topK);

            if (topChunks.isEmpty()) {
                return ResponseEntity.ok(new ChatResponse(
                        "I couldn't find anything relevant in the uploaded document for that question.",
                        List.of()));
            }

            String answer = llmService.generateAnswer(request.getQuestion(), topChunks);

            redisService.appendChatTurn(request.getSessionId(), request.getQuestion(), answer);

            return ResponseEntity.ok(new ChatResponse(answer, topChunks));

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "chat_failed",
                            "message", e.getMessage(),
                            "upstreamBody", e.getResponseBodyAsString()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "chat_failed", "message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestParam("sessionId") String sessionId) {
        List<ChatTurn> turns = redisService.getHistory(sessionId);
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "turns", turns));
    }
}
