package com.vocab.estimator.dto;

import java.util.List;

public class BatchSubmitDTO {
    private List<String> wordLines;
    private String textContent;

    public BatchSubmitDTO() {}

    public BatchSubmitDTO(List<String> wordLines, String textContent) {
        this.wordLines = wordLines;
        this.textContent = textContent;
    }

    public List<String> getWordLines() { return this.wordLines; }
    public void setWordLines(List<String> wordLines) { this.wordLines = wordLines; }
    public String getTextContent() { return this.textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }
}
