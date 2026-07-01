package com.vocab.estimator.dto;

import java.util.List;
import java.util.Map;

public class CorpusAnalysisDTO {
    private String corpusType;
    private Integer totalWords;
    private Map<String,Integer> levelDistribution;
    private EstimateResultDTO estimate;

    public CorpusAnalysisDTO() {}

    public CorpusAnalysisDTO(String corpusType, Integer totalWords, Map<String,Integer> levelDistribution, EstimateResultDTO estimate) {
        this.corpusType = corpusType;
        this.totalWords = totalWords;
        this.levelDistribution = levelDistribution;
        this.estimate = estimate;
    }

    public String getCorpusType() { return this.corpusType; }
    public void setCorpusType(String corpusType) { this.corpusType = corpusType; }
    public Integer getTotalWords() { return this.totalWords; }
    public void setTotalWords(Integer totalWords) { this.totalWords = totalWords; }
    public Map<String,Integer> getLevelDistribution() { return this.levelDistribution; }
    public void setLevelDistribution(Map<String,Integer> levelDistribution) { this.levelDistribution = levelDistribution; }
    public EstimateResultDTO getEstimate() { return this.estimate; }
    public void setEstimate(EstimateResultDTO estimate) { this.estimate = estimate; }
}
