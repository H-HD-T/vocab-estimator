package com.vocab.estimator.dto;

import java.util.List;

public class StatsDTO {
    private List<CorrelationItem> correlationItems;

    public StatsDTO() {}
    public StatsDTO(List<CorrelationItem> correlationItems) { this.correlationItems = correlationItems; }

    public List<CorrelationItem> getCorrelationItems() { return correlationItems; }
    public void setCorrelationItems(List<CorrelationItem> correlationItems) { this.correlationItems = correlationItems; }

    public static class CorrelationItem {
        private String studentCode;
        private Integer cet4Score;
        private Integer cet6Score;
        private Integer estimateVocab;
        private Double avgConfidence;

        public CorrelationItem() {}
        public CorrelationItem(String studentCode, Integer cet4Score, Integer cet6Score, Integer estimateVocab, Double avgConfidence) {
            this.studentCode = studentCode; this.cet4Score = cet4Score; this.cet6Score = cet6Score;
            this.estimateVocab = estimateVocab; this.avgConfidence = avgConfidence;
        }
        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
        public Integer getCet4Score() { return cet4Score; }
        public void setCet4Score(Integer cet4Score) { this.cet4Score = cet4Score; }
        public Integer getCet6Score() { return cet6Score; }
        public void setCet6Score(Integer cet6Score) { this.cet6Score = cet6Score; }
        public Integer getEstimateVocab() { return estimateVocab; }
        public void setEstimateVocab(Integer estimateVocab) { this.estimateVocab = estimateVocab; }
        public Double getAvgConfidence() { return avgConfidence; }
        public void setAvgConfidence(Double avgConfidence) { this.avgConfidence = avgConfidence; }
    }
}