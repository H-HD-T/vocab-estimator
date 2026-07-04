package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 1: Difficulty-Weighted Vocabulary Estimation
 *
 * Core logic:
 * 1. Divide test words into K/P/F/C difficulty levels
 * 2. Each level has a target vocabulary size summing to ~40000 (TVY-compatible)
 * 3. Estimate = sum of (known_ratio_per_level × level_target)
 * 4. Consistency: higher-level ratio cannot exceed lower-level by 1.5x
 * 5. Output Di naturally falls in 0-40000 range, no post-hoc calibration needed
 *
 * Level targets (additional words mastered at each stage):
 *   K:  2,000  (primary school)
 *   P:  5,000  (junior high, cumulative 7,000)
 *   F: 12,000  (senior high, cumulative 19,000)
 *   C: 21,000  (college/CET-6, cumulative 40,000)
 */
@Component
public class WordFrequencyEstimator implements VocabEstimator {

    private static final Map<String, Integer> LEVEL_TARGET = new LinkedHashMap<>();
    static {
        LEVEL_TARGET.put("K", 2000);
        LEVEL_TARGET.put("P", 5000);
        LEVEL_TARGET.put("F", 12000);
        LEVEL_TARGET.put("C", 21000);
    }

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }

        // Per-level stats: [knownCount, totalCount]
        Map<String, int[]> levelStats = new LinkedHashMap<>();
        for (String level : LEVEL_TARGET.keySet()) {
            levelStats.put(level, new int[]{0, 0});
        }

        int known = 0, unknown = 0;
        for (Map<String, Object> item : wordResults) {
            boolean isKnown = (Boolean) item.getOrDefault("known", false);
            String difficulty = (String) item.getOrDefault("difficulty", "K");
            if (!LEVEL_TARGET.containsKey(difficulty)) difficulty = "K";

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
        for (String level : LEVEL_TARGET.keySet()) {
            int[] stats = levelStats.get(level);
            levelKnownRatio.put(level, stats[1] > 0 ? (double) stats[0] / stats[1] : 0.5);
        }

        // Consistency constraint: higher level ratio capped by lower level
        String[] levels = {"K", "P", "F", "C"};
        double[] constrained = new double[4];
        for (int i = 0; i < 4; i++) {
            constrained[i] = levelKnownRatio.get(levels[i]);
            // Constraint: if lower level < 40%, cap higher level to 1.5x
            for (int j = 0; j < i; j++) {
                if (constrained[j] < 0.4 && constrained[i] > constrained[j] * 1.5) {
                    constrained[i] = constrained[j] * 1.5;
                }
            }
        }

        // Estimate = sigma(constrained_ratio × level_target)
        int estimate = 0;
        for (int i = 0; i < 4; i++) {
            estimate += Math.round(constrained[i] * LEVEL_TARGET.get(levels[i]));
        }
        estimate = Math.max(0, Math.min(estimate, 40000));

        // ---- Range and confidence (same logic, scaled to 40000 range) ----
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
        rangeWidth = Math.max(500, rangeWidth);

        int minRange = Math.max(0, estimate - rangeWidth / 2);
        int maxRange = Math.min(40000, estimate + rangeWidth / 2);

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
        return "Difficulty-Weighted Estimation (0-40000)";
    }
}
