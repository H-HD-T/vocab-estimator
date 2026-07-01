package com.vocab.estimator.dto;

public class EstimateResultDTO {
    private Integer estimate;
    private Integer minRange;
    private Integer maxRange;
    private Double confidence;
    private Integer knownCount;
    private Integer unknownCount;
    private Integer totalWords;

    public EstimateResultDTO() {}

    public EstimateResultDTO(Integer estimate, Integer minRange, Integer maxRange, Double confidence, Integer knownCount, Integer unknownCount, Integer totalWords) {
        this.estimate = estimate;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.confidence = confidence;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.totalWords = totalWords;
    }

    public Integer getEstimate() { return this.estimate; }
    public void setEstimate(Integer estimate) { this.estimate = estimate; }
    public Integer getMinRange() { return this.minRange; }
    public void setMinRange(Integer minRange) { this.minRange = minRange; }
    public Integer getMaxRange() { return this.maxRange; }
    public void setMaxRange(Integer maxRange) { this.maxRange = maxRange; }
    public Double getConfidence() { return this.confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Integer getKnownCount() { return this.knownCount; }
    public void setKnownCount(Integer knownCount) { this.knownCount = knownCount; }
    public Integer getUnknownCount() { return this.unknownCount; }
    public void setUnknownCount(Integer unknownCount) { this.unknownCount = unknownCount; }
    public Integer getTotalWords() { return this.totalWords; }
    public void setTotalWords(Integer totalWords) { this.totalWords = totalWords; }
}
