package com.ragbot.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/chat
 */
public class ChatRequest {

    @NotBlank(message = "question must not be blank")
    private String question;

    @NotBlank(message = "sessionId must not be blank")
    private String sessionId;

    public ChatRequest() {
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
