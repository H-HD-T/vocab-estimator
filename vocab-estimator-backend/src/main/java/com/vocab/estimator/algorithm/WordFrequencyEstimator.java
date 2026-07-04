package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 1: Ratio-Based Vocabulary Estimation (primary estimator)
 *
 * Core logic:
 * 1. Compute overall known ratio = known / total
 * 2. Compute difficulty-weighted ratio: known words at higher levels = higher weight
 *    weights: K=1, P=3, F=9, C=27 (exponential to amplify difficulty differences)
 * 3. diffFactor = avgKnownWeight / avgAllWeight, clamped to [0.5, 1.5]
 * 4. Estimate = known_ratio × 18000 × diffFactor
 * 5. Output Di in 0-40000 range
 */
@Component
public class WordFrequencyEstimator implements VocabEstimator {

    private static final int TOTAL_REFERENCE = 18000;
    private static final int MAX_ESTIMATE = 40000;

    // Exponential weights: knowing a C-level word is 27x as informative as K-level
    private static final Map<String, Integer> LEVEL_WEIGHT = new LinkedHashMap<>();
    static {
        LEVEL_WEIGHT.put("K", 1);
        LEVEL_WEIGHT.put("P", 3);
        LEVEL_WEIGHT.put("F", 9);
        LEVEL_WEIGHT.put("C", 27);
    }

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }

        int total = wordResults.size();
        int known = 0, unknown = 0;
        double weightedKnownSum = 0;
        double weightedTotalSum = 0;

        for (Map<String, Object> item : wordResults) {
            boolean isKnown = (Boolean) item.getOrDefault("known", false);
            String difficulty = (String) item.getOrDefault("difficulty", "K");
            if (!LEVEL_WEIGHT.containsKey(difficulty)) difficulty = "K";
            int weight = LEVEL_WEIGHT.get(difficulty);
            if (isKnown) { known++; weightedKnownSum += weight; }
            else { unknown++; }
            weightedTotalSum += weight;
        }

        double ratio = total > 0 ? (double) known / total : 0;

        // diffFactor: reflects difficulty of known words vs all words
        // >1 means known words are harder than average (higher vocabulary)
        // <1 means known words are easier than average (lower vocabulary)
        double diffFactor = 1.0;
        if (known > 0) {
            double avgKnownWeight = weightedKnownSum / known;
            double avgAllWeight = weightedTotalSum / total;
            diffFactor = avgAllWeight > 0 ? avgKnownWeight / avgAllWeight : 1.0;
            // Wider range to capture more variance
            diffFactor = Math.max(0.5, Math.min(1.5, diffFactor));
        }

        int estimate = (int) Math.round(ratio * TOTAL_REFERENCE * diffFactor);
        estimate = Math.max(0, Math.min(MAX_ESTIMATE, estimate));

        // Range
        double sampleFactor = Math.min(1.0, total / 40.0);
        int rangeWidth = (int) (estimate * 0.35 * (1.4 - sampleFactor * 0.5));
        rangeWidth = Math.max(500, rangeWidth);
        int minRange = Math.max(0, estimate - rangeWidth / 2);
        int maxRange = Math.min(MAX_ESTIMATE, estimate + rangeWidth / 2);

        // Confidence
        double sampleConf = Math.min(1.0, total / 50.0);
        double balanceConf = 1.0 - Math.abs(ratio - 0.5) * 1.2;
        balanceConf = Math.max(0.2, Math.min(1.0, balanceConf));
        double consistencyConf = 1.0 - Math.abs(diffFactor - 1.0) * 0.5;
        consistencyConf = Math.max(0.3, Math.min(1.0, consistencyConf));
        double confidence = (sampleConf * 0.35 + balanceConf * 0.35 + consistencyConf * 0.3) * 100;
        confidence = Math.max(5, Math.min(100, confidence));

        return new AlgorithmResult(estimate, minRange, maxRange, confidence, known, unknown, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Ratio-Based (TVY-fitted)";
    }
}
