package com.ragbot.model;

import java.util.List;

/**
 * Response body for POST /api/chat
 */
public class ChatResponse {

    private String answer;
    private List<SourceChunk> sources;

    public ChatResponse() {
    }

    public ChatResponse(String answer, List<SourceChunk> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<SourceChunk> getSources() {
        return sources;
    }

    public void setSources(List<SourceChunk> sources) {
        this.sources = sources;
    }
}
