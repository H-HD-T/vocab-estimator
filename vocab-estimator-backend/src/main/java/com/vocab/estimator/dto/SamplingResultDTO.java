package com.vocab.estimator.dto;

import java.util.List;

public class SamplingResultDTO {
    private Integer sampleLength;
    private Integer knowRatio;
    private Integer sampleCount;
    private Double meanEstimate;
    private Double variance;
    private List<Integer> allEstimates;

    public SamplingResultDTO() {}

    public SamplingResultDTO(Integer sampleLength, Integer knowRatio, Integer sampleCount, Double meanEstimate, Double variance, List<Integer> allEstimates) {
        this.sampleLength = sampleLength;
        this.knowRatio = knowRatio;
        this.sampleCount = sampleCount;
        this.meanEstimate = meanEstimate;
        this.variance = variance;
        this.allEstimates = allEstimates;
    }

    public Integer getSampleLength() { return this.sampleLength; }
    public void setSampleLength(Integer sampleLength) { this.sampleLength = sampleLength; }
    public Integer getKnowRatio() { return this.knowRatio; }
    public void setKnowRatio(Integer knowRatio) { this.knowRatio = knowRatio; }
    public Integer getSampleCount() { return this.sampleCount; }
    public void setSampleCount(Integer sampleCount) { this.sampleCount = sampleCount; }
    public Double getMeanEstimate() { return this.meanEstimate; }
    public void setMeanEstimate(Double meanEstimate) { this.meanEstimate = meanEstimate; }
    public Double getVariance() { return this.variance; }
    public void setVariance(Double variance) { this.variance = variance; }
    public List<Integer> getAllEstimates() { return this.allEstimates; }
    public void setAllEstimates(List<Integer> allEstimates) { this.allEstimates = allEstimates; }
}
