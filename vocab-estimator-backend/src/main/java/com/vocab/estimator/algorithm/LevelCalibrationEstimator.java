package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 2: Hierarchical Level Calibration (cumulative model)
 *
 * Core logic:
 * 1. Use cumulative vocabulary at each level (hierarchical, sums to ~40000)
 * 2. Determine user tier based on highest level with >= 20% recognition
 * 3. Validate: for high tiers (F/C), lower levels with data must be >= 80%
 * 4. Estimate = lower_bound + known_ratio × (upper_bound - lower_bound)
 * 5. For levels with NO test data, use overall known ratio from levels with data
 * 6. Output Di naturally falls in 0-40000 range, no post-hoc calibration
 *
 * Cumulative vocab by level:
 *   K:  2,000  (mastered up to primary school)
 *   P:  7,000  (mastered up to junior high)
 *   F: 19,000  (mastered up to senior high/gaokao)
 *   C: 40,000  (mastered up to college/CET-6)
 *
 * Key improvement: Empty levels use overall known ratio instead of fixed 0.5.
 * Candidate threshold lowered to 0.2 for better inclusion of ~30% known-rate users.
 */
@Component
public class LevelCalibrationEstimator implements VocabEstimator {

    private static final Map<String, Integer> CUMULATIVE_VOCAB = new LinkedHashMap<>();
    static {
        CUMULATIVE_VOCAB.put("K", 2000);
        CUMULATIVE_VOCAB.put("P", 7000);
        CUMULATIVE_VOCAB.put("F", 19000);
        CUMULATIVE_VOCAB.put("C", 40000);
    }

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }

        // Per-level results
        Map<String, List<Boolean>> levelResults = new HashMap<>();
        for (String level : CUMULATIVE_VOCAB.keySet()) {
            levelResults.put(level, new ArrayList<>());
        }

        int known = 0, unknown = 0;
        for (Map<String, Object> item : wordResults) {
            String difficulty = (String) item.getOrDefault("difficulty", "K");
            boolean isKnown = (Boolean) item.getOrDefault("known", false);
            if (!levelResults.containsKey(difficulty)) difficulty = "K";
            levelResults.get(difficulty).add(isKnown);
            if (isKnown) known++;
            else unknown++;
        }

        int total = wordResults.size();
        double overallKnownRatio = total > 0 ? (double) known / total : 0.5;

        // Per-level recognition rates
        Map<String, Double> levelRates = new LinkedHashMap<>();
        Map<String, Boolean> levelHasData = new HashMap<>();
        for (String level : CUMULATIVE_VOCAB.keySet()) {
            List<Boolean> results = levelResults.get(level);
            if (results.isEmpty()) {
                // === FIXED: Use overall known ratio for empty levels ===
                levelRates.put(level, overallKnownRatio);
                levelHasData.put(level, false);
            } else {
                long knownCount = results.stream().filter(r -> r).count();
                levelRates.put(level, (double) knownCount / results.size());
                levelHasData.put(level, true);
            }
        }

        // Find candidate tier: highest level with >= 20% recognition
        // Progressive thresholds: K>=0.1, P>=0.2, F>=0.35, C>=0.5
        String candidateLevel = "K";
        for (String level : CUMULATIVE_VOCAB.keySet()) {
            // Only use actual data levels for candidate decision, not proxy-filled ones
            if (!levelHasData.get(level)) continue;
            double levelThreshold = level.equals("K") ? 0.1 : (level.equals("P") ? 0.2 : (level.equals("F") ? 0.35 : 0.5));
            if (levelRates.get(level) >= levelThreshold) {
                candidateLevel = level;
            }
        }

        // Validation: for high tiers (F/C), lower levels must be >= 80%
        boolean validCalibration = true;
        if (candidateLevel.equals("C") || candidateLevel.equals("F")) {
            for (String level : CUMULATIVE_VOCAB.keySet()) {
                if (level.equals(candidateLevel)) break;
                if (levelHasData.get(level) && levelRates.getOrDefault(level, 0.0) < 0.8) {
                    validCalibration = false;
                    break;
                }
            }
        }

        // Interpolate estimate within candidate tier
        List<String> levelOrder = new ArrayList<>(CUMULATIVE_VOCAB.keySet());
        int candidateIndex = levelOrder.indexOf(candidateLevel);
        double candidateRate = levelRates.get(candidateLevel);

        int lowerBound;
        if (candidateIndex <= 0) {
            lowerBound = 0;
        } else {
            lowerBound = CUMULATIVE_VOCAB.get(levelOrder.get(candidateIndex - 1));
        }
        int upperBound = CUMULATIVE_VOCAB.get(candidateLevel);

        int segmentSize = upperBound - lowerBound;
        int estimate = lowerBound + (int) Math.round(candidateRate * segmentSize);
        estimate = Math.max(0, Math.min(estimate, 40000));

        // Range
        double rateSpread = Math.abs(candidateRate - 0.7);
        int rangeWidth = (int) (estimate * 0.25 * (1.0 + rateSpread * 2));
        rangeWidth = Math.max(500, rangeWidth);

        int minRange = Math.max(0, estimate - rangeWidth / 2);
        int maxRange = Math.min(40000, estimate + rangeWidth / 2);

        // Confidence
        double sampleConf = Math.min(1.0, total / 60.0);
        double tierConf = 1.0 - (levelOrder.indexOf(candidateLevel) / 4.0) * 0.3;
        double validConf = validCalibration ? 0.25 : 0.05;

        double confidence = (sampleConf * 0.4 + tierConf * 0.25 + validConf * 0.2) * 100;
        confidence = Math.max(5, Math.min(100, confidence));

        return new AlgorithmResult(estimate, minRange, maxRange, confidence, known, unknown, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Hierarchical Calibration (0-40000)";
    }
}
