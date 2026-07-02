
package com.vocab.estimator.dto;

import java.util.List;

public class StabilityResultDTO {
    private List<StabilityComboResult> combos;
    
    public StabilityResultDTO() {}
    public StabilityResultDTO(List<StabilityComboResult> combos) { this.combos = combos; }
    public List<StabilityComboResult> getCombos() { return combos; }
    public void setCombos(List<StabilityComboResult> combos) { this.combos = combos; }
    
    public static class StabilityComboResult {
        private Integer knowRatio;       // e.g. 10, 20, 30
        private Integer sampleLength;    // e.g. 200, 300, 400
        private Integer runCount;        // always 100
        private Double meanEstimate;
        private Double variance;
        private List<Integer> allEstimates;
        
        public StabilityComboResult() {}
        public StabilityComboResult(Integer knowRatio, Integer sampleLength, Integer runCount, 
            Double meanEstimate, Double variance, List<Integer> allEstimates) {
            this.knowRatio = knowRatio;
            this.sampleLength = sampleLength;
            this.runCount = runCount;
            this.meanEstimate = meanEstimate;
            this.variance = variance;
            this.allEstimates = allEstimates;
        }
        
        public Integer getKnowRatio() { return knowRatio; }
        public void setKnowRatio(Integer knowRatio) { this.knowRatio = knowRatio; }
        public Integer getSampleLength() { return sampleLength; }
        public void setSampleLength(Integer sampleLength) { this.sampleLength = sampleLength; }
        public Integer getRunCount() { return runCount; }
        public void setRunCount(Integer runCount) { this.runCount = runCount; }
        public Double getMeanEstimate() { return meanEstimate; }
        public void setMeanEstimate(Double meanEstimate) { this.meanEstimate = meanEstimate; }
        public Double getVariance() { return variance; }
        public void setVariance(Double variance) { this.variance = variance; }
        public List<Integer> getAllEstimates() { return allEstimates; }
        public void setAllEstimates(List<Integer> allEstimates) { this.allEstimates = allEstimates; }
    }
}
