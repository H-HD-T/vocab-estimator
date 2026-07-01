package com.vocab.estimator.algorithm;

public class AlgorithmResult {
    private int estimate;
    private int minRange;
    private int maxRange;
    private double confidence;
    private int knownCount;
    private int unknownCount;
    private int totalWords;

    public AlgorithmResult() {}

    public AlgorithmResult(int estimate, int minRange, int maxRange, double confidence, int knownCount, int unknownCount, int totalWords) {
        this.estimate = estimate;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.confidence = confidence;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.totalWords = totalWords;
    }

    public int getEstimate() { return this.estimate; }
    public void setEstimate(int estimate) { this.estimate = estimate; }
    public int getMinRange() { return this.minRange; }
    public void setMinRange(int minRange) { this.minRange = minRange; }
    public int getMaxRange() { return this.maxRange; }
    public void setMaxRange(int maxRange) { this.maxRange = maxRange; }
    public double getConfidence() { return this.confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public int getKnownCount() { return this.knownCount; }
    public void setKnownCount(int knownCount) { this.knownCount = knownCount; }
    public int getUnknownCount() { return this.unknownCount; }
    public void setUnknownCount(int unknownCount) { this.unknownCount = unknownCount; }
    public int getTotalWords() { return this.totalWords; }
    public void setTotalWords(int totalWords) { this.totalWords = totalWords; }
}
