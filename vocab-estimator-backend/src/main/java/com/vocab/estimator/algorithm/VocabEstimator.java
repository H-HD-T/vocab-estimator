package com.vocab.estimator.algorithm;

import java.util.List;
import java.util.Map;

/**
 * Vocabulary estimation algorithm interface
 * All estimation algorithms must implement this interface
 */
public interface VocabEstimator {
    /**
     * Estimate vocabulary size based on test results
     * @param wordResults list of {word, known(true/false), difficulty, frequency}
     * @return AlgorithmResult with estimate, range, and confidence
     */
    AlgorithmResult estimate(List<Map<String, Object>> wordResults);
    
    /**
     * Get the name/description of this algorithm
     */
    String getAlgorithmName();
}
