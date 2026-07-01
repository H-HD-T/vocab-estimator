package com.vocab.estimator.dto;

import java.util.List;

public class BatchResultDTO {
    private List<BatchItemResult> results;
    private String taskId;

    public BatchResultDTO() {}
    public BatchResultDTO(List<BatchItemResult> results, String taskId) {
        this.results = results; this.taskId = taskId;
    }
    public List<BatchItemResult> getResults() { return results; }
    public void setResults(List<BatchItemResult> results) { this.results = results; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public static class BatchItemResult {
        private String wordLine;
        private EstimateResultDTO estimate;

        public BatchItemResult() {}
        public BatchItemResult(String wordLine, EstimateResultDTO estimate) {
            this.wordLine = wordLine; this.estimate = estimate;
        }
        public String getWordLine() { return wordLine; }
        public void setWordLine(String wordLine) { this.wordLine = wordLine; }
        public EstimateResultDTO getEstimate() { return estimate; }
        public void setEstimate(EstimateResultDTO estimate) { this.estimate = estimate; }
    }
}
