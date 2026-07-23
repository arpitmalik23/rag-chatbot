package com.ragbot.service;

import com.ragbot.model.DocChunk;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Extracts text from an uploaded PDF and splits it into overlapping
 * chunks suitable for embedding.
 *
 * Chunking is word-based, which approximates tokens closely enough for
 * English prose (~0.75 words per token in practice, so 500 "tokens" is
 * treated here as 500 words to keep the implementation dependency-free;
 * swap in a real tokenizer if you need exact GPT/Gemini token counts).
 */
@Service
public class PdfService {

    private static final int CHUNK_SIZE_WORDS = 500;
    private static final int CHUNK_OVERLAP_WORDS = 50;

    /**
     * Extracts text page-by-page, then chunks it with overlap. Each chunk
     * records the page it started on so the frontend can cite a page number.
     */
    public List<DocChunk> extractAndChunk(MultipartFile file, String docId, String sessionId) throws IOException {
        List<String> pageTexts = extractPerPage(file);
        return chunkPages(pageTexts, docId, sessionId);
    }

    private List<String> extractPerPage(MultipartFile file) throws IOException {
        List<String> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);
                pages.add(text == null ? "" : text.trim());
            }
        }
        return pages;
    }

    private List<DocChunk> chunkPages(List<String> pageTexts, String docId, String sessionId) {
        List<DocChunk> chunks = new ArrayList<>();

        // Flatten to a single list of words, remembering which page each word came from.
        List<String> words = new ArrayList<>();
        List<Integer> wordPages = new ArrayList<>();
        for (int pageIdx = 0; pageIdx < pageTexts.size(); pageIdx++) {
            String[] pageWords = pageTexts.get(pageIdx).split("\\s+");
            for (String w : pageWords) {
                if (w.isBlank()) continue;
                words.add(w);
                wordPages.add(pageIdx + 1); // 1-indexed page number
            }
        }

        if (words.isEmpty()) {
            return chunks;
        }

        int step = CHUNK_SIZE_WORDS - CHUNK_OVERLAP_WORDS;
        int chunkIndex = 0;

        for (int start = 0; start < words.size(); start += step) {
            int end = Math.min(start + CHUNK_SIZE_WORDS, words.size());
            String text = String.join(" ", words.subList(start, end));
            int page = wordPages.get(start);

            chunks.add(new DocChunk(
                    UUID.randomUUID().toString(),
                    docId,
                    sessionId,
                    text,
                    page,
                    chunkIndex++
            ));

            if (end == words.size()) break;
        }

        return chunks;
    }
}
