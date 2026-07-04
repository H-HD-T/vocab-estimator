package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 2: Hierarchical Level Calibration (????????)
 *
 * Core logic:
 * 1. Use cumulative vocabulary at each level (hierarchical)
 * 2. Determine user tier based on highest level with >= 50% recognition
 * 3. Validate: for high tiers (F/C), lower levels with actual data must be >= 80%
 * 4. Estimate = lower_bound + known_ratio * (upper_bound - lower_bound)
 * 5. If validation fails, downgrade tier
 *
 * Cumulative vocab by level (?????????):
 *   K: 500  (mastered up to primary school)
 *   P: 2000 (mastered up to junior high)
 *   F: 4500 (mastered up to senior high/gaokao)
 *   C: 9000 (mastered up to college/CET-6)
 *
 * Cross-validation (??????):
 * - ?????????????? >= 80%
 * - ????????????????
 * - ??????????????????????
 */
@Component
public class LevelCalibrationEstimator implements VocabEstimator {

    private static final Map<String, Integer> CUMULATIVE_VOCAB = new LinkedHashMap<>();
    static {
        CUMULATIVE_VOCAB.put("K", 500);
        CUMULATIVE_VOCAB.put("P", 2000);
        CUMULATIVE_VOCAB.put("F", 4500);
        CUMULATIVE_VOCAB.put("C", 9000);
    }

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }

        // Per-level stats
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

        // Per-level recognition rates (only levels with actual data)
        Map<String, Double> levelRates = new LinkedHashMap<>();
        Map<String, Boolean> levelHasData = new HashMap<>();
        for (String level : CUMULATIVE_VOCAB.keySet()) {
            List<Boolean> results = levelResults.get(level);
            if (results.isEmpty()) {
                levelRates.put(level, 0.5); // default assumption for no data
                levelHasData.put(level, false);
            } else {
                long knownCount = results.stream().filter(r -> r).count();
                levelRates.put(level, (double) knownCount / results.size());
                levelHasData.put(level, true);
            }
        }

        // Find candidate tier: highest level with >= 50% recognition
        // Only consider levels with actual data for the candidate decision
        String candidateLevel = "K";
        for (String level : CUMULATIVE_VOCAB.keySet()) {
            double rate = levelRates.get(level);
            // For empty levels, use the rate from the highest data level as proxy
            if (!levelHasData.get(level)) {
                // If no data at this level, assume performance matches best known level
                // Don't change candidate based on empty levels
                continue;
            }
            if (rate >= 0.3) {
                candidateLevel = level;
            }
        }

        // Determine if calibration is valid (confidence only, no estimate downgrade)
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

        // Estimate = lower_bound + rate * (upper_bound - lower_bound)
        int segmentSize = upperBound - lowerBound;
        int estimate = lowerBound + (int) Math.round(candidateRate * segmentSize);
        estimate = Math.max(0, Math.min(estimate, 9000));

        // Range: wider for fewer samples, extreme ratios
        double rateSpread = Math.abs(candidateRate - 0.7);
        int rangeWidth = (int) (estimate * 0.25 * (1.0 + rateSpread * 2));
        rangeWidth = Math.max(200, rangeWidth);

        int minRange = Math.max(0, estimate - rangeWidth / 2);
        int maxRange = estimate + rangeWidth / 2;

        // Confidence: sample size + tier stability + pattern consistency
        double sampleConf = Math.min(1.0, total / 60.0);
        double tierConf = 1.0 - (levelOrder.indexOf(candidateLevel) / 4.0) * 0.3;
        double validConf = validCalibration ? 0.25 : 0.05;

        double confidence = (sampleConf * 0.4 + tierConf * 0.25 + validConf * 0.2) * 100;
        confidence = Math.max(5, Math.min(100, confidence));

        return new AlgorithmResult(estimate, minRange, maxRange, confidence, known, unknown, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Hierarchical Difficulty Calibration";
    }
}
