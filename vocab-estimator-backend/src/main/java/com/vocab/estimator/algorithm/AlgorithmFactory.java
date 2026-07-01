package com.vocab.estimator.algorithm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AlgorithmFactory {

    @Autowired
    public WordFrequencyEstimator frequencyEstimator;

    @Autowired
    public LevelCalibrationEstimator calibrationEstimator;

    public List<VocabEstimator> getAllAlgorithms() {
        return Arrays.asList(frequencyEstimator, calibrationEstimator);
    }

    public VocabEstimator getAlgorithm(String name) {
        for (VocabEstimator algo : getAllAlgorithms()) {
            if (algo.getAlgorithmName().equals(name)) return algo;
        }
        return frequencyEstimator;
    }

    public AlgorithmResult estimateAll(List<Map<String, Object>> wordResults) {
        List<AlgorithmResult> results = getAllAlgorithms().stream()
            .map(a -> a.estimate(wordResults)).toList();
        int avgEst = (int) results.stream().mapToInt(AlgorithmResult::getEstimate).average().orElse(0);
        int minR = results.stream().mapToInt(AlgorithmResult::getMinRange).min().orElse(0);
        int maxR = results.stream().mapToInt(AlgorithmResult::getMaxRange).max().orElse(0);
        double avgConf = results.stream().mapToDouble(AlgorithmResult::getConfidence).average().orElse(0);
        AlgorithmResult combined = results.isEmpty() ? new AlgorithmResult() : results.get(0);
        combined.setEstimate(avgEst);
        combined.setMinRange(minR);
        combined.setMaxRange(maxR);
        combined.setConfidence(avgConf);
        return combined;
    }
}
