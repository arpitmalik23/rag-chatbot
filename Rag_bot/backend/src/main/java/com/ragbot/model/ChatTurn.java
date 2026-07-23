package com.ragbot.model;

/**
 * A single question/answer pair, cached in Redis under the session's
 * history list so the chat can be restored on reload.
 */
public class ChatTurn {

    private String question;
    private String answer;
    private long timestamp;

    public ChatTurn() {
    }

    public ChatTurn(String question, String answer, long timestamp) {
        this.question = question;
        this.answer = answer;
        this.timestamp = timestamp;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
