package com.ragbot.controller;

import com.ragbot.model.DocChunk;
import com.ragbot.model.UploadResponse;
import com.ragbot.service.EmbeddingService;
import com.ragbot.service.PdfService;
import com.ragbot.service.RedisService;
import com.ragbot.service.VectorStoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final PdfService pdfService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final RedisService redisService;

    public UploadController(PdfService pdfService,
                             EmbeddingService embeddingService,
                             VectorStoreService vectorStoreService,
                             RedisService redisService) {
        this.pdfService = pdfService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.redisService = redisService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file,
                                        @RequestParam("sessionId") String sessionId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("File is empty."));
        }
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest().body(errorBody("Only PDF files are supported."));
        }

        String docId = UUID.randomUUID().toString();

        try {
            List<DocChunk> chunks = pdfService.extractAndChunk(file, docId, sessionId);

            if (chunks.isEmpty()) {
                return ResponseEntity.badRequest().body(errorBody("Couldn't extract any text from this PDF."));
            }

            List<String> texts = chunks.stream().map(DocChunk::getText).toList();
            List<List<Float>> embeddings = embeddingService.embedBatch(texts);

            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i).setEmbedding(embeddings.get(i));
            }
                          
          
            vectorStoreService.upsert(chunks);
            redisService.saveDocMetadata(sessionId, docId, file.getOriginalFilename(), chunks.size());

            return ResponseEntity.ok(new UploadResponse(docId, file.getOriginalFilename(), chunks.size(), sessionId));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Failed to read PDF: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Upload failed: " + e.getMessage()));
        }
    }

    private Map<String, String> errorBody(String message) {
        return Map.of("error", "upload_failed", "message", message);
    }
}
