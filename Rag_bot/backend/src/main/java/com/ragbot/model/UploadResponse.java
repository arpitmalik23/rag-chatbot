package com.ragbot.model;

/**
 * Response body for POST /api/upload
 */
public class UploadResponse {

    private String docId;
    private String filename;
    private int chunkCount;
    private String sessionId;

    public UploadResponse() {
    }

    public UploadResponse(String docId, String filename, int chunkCount, String sessionId) {
        this.docId = docId;
        this.filename = filename;
        this.chunkCount = chunkCount;
        this.sessionId = sessionId;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
