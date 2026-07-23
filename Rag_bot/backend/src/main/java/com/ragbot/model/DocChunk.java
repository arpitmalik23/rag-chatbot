package com.ragbot.model;

import java.util.List;

/**
 * A single chunk of a source document, ready to be embedded and stored
 * in the vector database.
 */
public class DocChunk {

    private String id;          // UUID, also used as the Qdrant point id
    private String docId;
    private String sessionId;
    private String text;
    private int page;
    private int chunkIndex;
    private List<Float> embedding; // populated after EmbeddingService runs, transient for storage calls

    public DocChunk() {
    }

    public DocChunk(String id, String docId, String sessionId, String text, int page, int chunkIndex) {
        this.id = id;
        this.docId = docId;
        this.sessionId = sessionId;
        this.text = text;
        this.page = page;
        this.chunkIndex = chunkIndex;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }
}
