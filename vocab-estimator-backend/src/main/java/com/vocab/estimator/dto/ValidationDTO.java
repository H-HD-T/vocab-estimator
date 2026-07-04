package com.vocab.estimator.dto;

import java.util.List;

public class ValidationDTO {
    private List<ValidationItem> items;
    private Double meanError;
    private Double meanBias;
    private Double correlation;
    private Integer sampleCount;

    // New detailed stats
    private Double mse;
    private Double rmse;
    private Double maxError;
    private Double minError;
    private Double meanRelativeError;
    
    // Scale normalization info
    private Integer ourMaxVocab;
    private Integer tvyMaxVocab;
    private Double scaleFactor;

    private ErrorDistribution errorDistribution;
    private List<ChartPoint> scatterData;
    private List<DistributionBin> histogramData;
    private String scatterChartUrl;
    private String histogramChartUrl;

    public ValidationDTO() {}
    public ValidationDTO(List<ValidationItem> items, Double meanError,
                        Double meanBias, Double correlation, Integer sampleCount) {
        this.items = items; this.meanError = meanError;
        this.meanBias = meanBias; this.correlation = correlation; this.sampleCount = sampleCount;
    }

    public List<ValidationItem> getItems() { return items; }
    public void setItems(List<ValidationItem> items) { this.items = items; }
    public Double getMeanError() { return meanError; }
    public void setMeanError(Double meanError) { this.meanError = meanError; }
    public Double getMeanBias() { return meanBias; }
    public void setMeanBias(Double meanBias) { this.meanBias = meanBias; }
    public Double getCorrelation() { return correlation; }
    public void setCorrelation(Double correlation) { this.correlation = correlation; }
    public Integer getSampleCount() { return sampleCount; }
    public void setSampleCount(Integer sampleCount) { this.sampleCount = sampleCount; }
    public Double getMse() { return mse; }
    public void setMse(Double mse) { this.mse = mse; }
    public Double getRmse() { return rmse; }
    public void setRmse(Double rmse) { this.rmse = rmse; }
    public Double getMaxError() { return maxError; }
    public void setMaxError(Double maxError) { this.maxError = maxError; }
    public Double getMinError() { return minError; }
    public void setMinError(Double minError) { this.minError = minError; }
    public Double getMeanRelativeError() { return meanRelativeError; }
    public void setMeanRelativeError(Double meanRelativeError) { this.meanRelativeError = meanRelativeError; }
    public Integer getOurMaxVocab() { return ourMaxVocab; }
    public void setOurMaxVocab(Integer ourMaxVocab) { this.ourMaxVocab = ourMaxVocab; }
    public Integer getTvyMaxVocab() { return tvyMaxVocab; }
    public void setTvyMaxVocab(Integer tvyMaxVocab) { this.tvyMaxVocab = tvyMaxVocab; }
    public Double getScaleFactor() { return scaleFactor; }
    public void setScaleFactor(Double scaleFactor) { this.scaleFactor = scaleFactor; }
    public ErrorDistribution getErrorDistribution() { return errorDistribution; }
    public void setErrorDistribution(ErrorDistribution errorDistribution) { this.errorDistribution = errorDistribution; }
    public List<ChartPoint> getScatterData() { return scatterData; }
    public void setScatterData(List<ChartPoint> scatterData) { this.scatterData = scatterData; }
    public List<DistributionBin> getHistogramData() { return histogramData; }
    public void setHistogramData(List<DistributionBin> histogramData) { this.histogramData = histogramData; }
    public String getScatterChartUrl() { return scatterChartUrl; }
    public void setScatterChartUrl(String scatterChartUrl) { this.scatterChartUrl = scatterChartUrl; }
    public String getHistogramChartUrl() { return histogramChartUrl; }
    public void setHistogramChartUrl(String histogramChartUrl) { this.histogramChartUrl = histogramChartUrl; }

    public static class ValidationItem {
        private List<String> knownWords;
        private List<String> unknownWords;
        private Integer standardEstimate;    // Ci from TVY
        private Integer algorithmEstimate;  // Di proportion-based (for comparison with TVY)
        private Integer rawAlgorithmEstimate; // Di from our actual algorithm
        private Integer normalizedEstimate; // Scaled to TVY range
        private Integer diff;
        private Integer absoluteError;
        private Double relativeError;

        public ValidationItem() {}
        public ValidationItem(List<String> knownWords, List<String> unknownWords,
                            Integer standardEstimate, Integer algorithmEstimate,
                            Integer rawAlgorithmEstimate,
                            Integer normalizedEstimate, Integer diff,
                            Integer absoluteError, Double relativeError) {
            this.knownWords = knownWords; this.unknownWords = unknownWords;
            this.standardEstimate = standardEstimate; this.algorithmEstimate = algorithmEstimate;
            this.rawAlgorithmEstimate = rawAlgorithmEstimate;
            this.normalizedEstimate = normalizedEstimate;
            this.diff = diff; this.absoluteError = absoluteError;
            this.relativeError = relativeError;
        }
        
        public Integer getRawAlgorithmEstimate() { return rawAlgorithmEstimate; }
        public void setRawAlgorithmEstimate(Integer rawAlgorithmEstimate) { this.rawAlgorithmEstimate = rawAlgorithmEstimate; }

        public List<String> getKnownWords() { return knownWords; }
        public void setKnownWords(List<String> knownWords) { this.knownWords = knownWords; }
        public List<String> getUnknownWords() { return unknownWords; }
        public void setUnknownWords(List<String> unknownWords) { this.unknownWords = unknownWords; }
        public Integer getStandardEstimate() { return standardEstimate; }
        public void setStandardEstimate(Integer standardEstimate) { this.standardEstimate = standardEstimate; }
        public Integer getAlgorithmEstimate() { return algorithmEstimate; }
        public void setAlgorithmEstimate(Integer algorithmEstimate) { this.algorithmEstimate = algorithmEstimate; }
        public Integer getNormalizedEstimate() { return normalizedEstimate; }
        public void setNormalizedEstimate(Integer normalizedEstimate) { this.normalizedEstimate = normalizedEstimate; }
        public Integer getDiff() { return diff; }
        public void setDiff(Integer diff) { this.diff = diff; }
        public Integer getAbsoluteError() { return absoluteError; }
        public void setAbsoluteError(Integer absoluteError) { this.absoluteError = absoluteError; }
        public Double getRelativeError() { return relativeError; }
        public void setRelativeError(Double relativeError) { this.relativeError = relativeError; }
    }

    public static class ErrorDistribution {
        private int within500;
        private int within1000;
        private int within2000;
        private int beyond2000;

        public ErrorDistribution() {}
        public ErrorDistribution(int within500, int within1000, int within2000, int beyond2000) {
            this.within500 = within500; this.within1000 = within1000;
            this.within2000 = within2000; this.beyond2000 = beyond2000;
        }
        public int getWithin500() { return within500; }
        public void setWithin500(int within500) { this.within500 = within500; }
        public int getWithin1000() { return within1000; }
        public void setWithin1000(int within1000) { this.within1000 = within1000; }
        public int getWithin2000() { return within2000; }
        public void setWithin2000(int within2000) { this.within2000 = within2000; }
        public int getBeyond2000() { return beyond2000; }
        public void setBeyond2000(int beyond2000) { this.beyond2000 = beyond2000; }
    }

    public static class ChartPoint {
        private double x;
        private double y;
        public ChartPoint() {}
        public ChartPoint(double x, double y) { this.x = x; this.y = y; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
    }

    public static class DistributionBin {
        private double min;
        private double max;
        private int count;
        public DistributionBin() {}
        public DistributionBin(double min, double max, int count) { this.min = min; this.max = max; this.count = count; }
        public double getMin() { return min; }
        public void setMin(double min) { this.min = min; }
        public double getMax() { return max; }
        public void setMax(double max) { this.max = max; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
