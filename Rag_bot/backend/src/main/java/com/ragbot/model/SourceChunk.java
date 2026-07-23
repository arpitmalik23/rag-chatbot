package com.ragbot.model;

/**
 * A retrieved chunk returned alongside the answer, so the frontend can
 * show which part of the document the model relied on.
 */
public class SourceChunk {

    private String text;
    private int page;
    private double score;

    public SourceChunk() {
    }

    public SourceChunk(String text, int page, double score) {
        this.text = text;
        this.page = page;
        this.score = score;
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

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
