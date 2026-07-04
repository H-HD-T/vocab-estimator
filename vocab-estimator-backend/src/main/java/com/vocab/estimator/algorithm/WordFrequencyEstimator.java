package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 1: Per-Level Cumulative Vocabulary Estimation
 *
 * Core logic:
 * 1. Divide vocabulary into K/P/F/C difficulty levels
 * 2. Each level has a vocabulary base for Chinese ESL learners
 * 3. Estimate = sum of (known_ratio * level_base) per level
 * 4. Consistency: higher-level ratio cannot exceed lower-level by 1.5x
 * 5. Confidence from sample size + distribution balance
 *
 * Level bases (additional words at each stage):
 *   K(primary): 500
 *   P(junior): 1500 (cumulative 2000)
 *   F(senior): 2500 (cumulative 4500, gaokao level)
 *   C(college): 4500 (cumulative 9000, CET-6 level)
 */
@Component
public class WordFrequencyEstimator implements VocabEstimator {

    private static final Map<String, Integer> LEVEL_BASE_VOCAB = new LinkedHashMap<>();
    static {
        LEVEL_BASE_VOCAB.put("K", 500);
        LEVEL_BASE_VOCAB.put("P", 1500);
        LEVEL_BASE_VOCAB.put("F", 2500);
        LEVEL_BASE_VOCAB.put("C", 4500);
    }

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }

        Map<String, int[]> levelStats = new LinkedHashMap<>();
        for (String level : LEVEL_BASE_VOCAB.keySet()) {
            levelStats.put(level, new int[]{0, 0});
        }

        int known = 0, unknown = 0;
        for (Map<String, Object> item : wordResults) {
            boolean isKnown = (Boolean) item.getOrDefault("known", false);
            String difficulty = (String) item.getOrDefault("difficulty", "K");
            if (!LEVEL_BASE_VOCAB.containsKey(difficulty)) difficulty = "K";

            int[] stats = levelStats.get(difficulty);
            stats[1]++;
            if (isKnown) {
                stats[0]++;
                known++;
            } else {
                unknown++;
            }
        }

        int total = wordResults.size();

        // Compute per-level known ratios
        Map<String, Double> levelKnownRatio = new LinkedHashMap<>();
        for (String level : LEVEL_BASE_VOCAB.keySet()) {
            int[] stats = levelStats.get(level);
            levelKnownRatio.put(level, stats[1] > 0 ? (double) stats[0] / stats[1] : 0.5);
        }

        // Consistency constraint: higher level ratio capped by lower level
        String[] levels = {"K", "P", "F", "C"};
        double[] constrained = new double[4];
        for (int i = 0; i < 4; i++) {
            constrained[i] = levelKnownRatio.get(levels[i]);
            // Constraint 1: if lower level < 40%, cap higher level to 1.5x
            for (int j = 0; j < i; j++) {
                if (constrained[j] < 0.4 && constrained[i] > constrained[j] * 1.5) {
                    constrained[i] = constrained[j] * 1.5;
                }
            }

        }

        // Estimate = sigma(constrained_ratio * level_base)
        int estimate = 0;
        for (int i = 0; i < 4; i++) {
            estimate += Math.round(constrained[i] * LEVEL_BASE_VOCAB.get(levels[i]));
        }
        estimate = Math.max(0, Math.min(estimate, 9000));

        // Range calculation
        double sampleFactor = Math.min(1.0, total / 40.0);
        double knownTotal = (double) known / total;
        double extremeFactor = 1.0 - Math.abs(knownTotal - 0.5) * 0.8;

        double ratioStdDev = 0;
        double meanRatio = Arrays.stream(constrained).average().orElse(0.5);
        for (double r : constrained) {
            ratioStdDev += Math.pow(r - meanRatio, 2);
        }
        ratioStdDev = Math.sqrt(ratioStdDev / 4);
        double distributionFactor = 1.0 + ratioStdDev * 0.5;

        int rangeWidth = (int) (estimate * 0.3 * (1.4 - sampleFactor * 0.5) * distributionFactor);
        rangeWidth = Math.max(200, rangeWidth);

        int minRange = Math.max(0, estimate - rangeWidth / 2);
        int maxRange = estimate + rangeWidth / 2;

        // Confidence calculation
        double sampleConf = Math.min(1.0, total / 50.0);
        double balanceConf = 1.0 - Math.abs(knownTotal - 0.5) * 1.2;
        balanceConf = Math.max(0.2, Math.min(1.0, balanceConf));
        double consistencyConf = 1.0 - ratioStdDev * 0.5;
        consistencyConf = Math.max(0.3, Math.min(1.0, consistencyConf));

        double confidence = (sampleConf * 0.35 + balanceConf * 0.35 + consistencyConf * 0.3) * 100;
        confidence = Math.max(5, Math.min(100, confidence));

        return new AlgorithmResult(estimate, minRange, maxRange, confidence, known, unknown, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Word Frequency Weighted Estimation";
    }
}
