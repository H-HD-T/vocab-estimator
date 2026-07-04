package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 2: Lightweight Level Calibration (complementary estimator)
 *
 * Core logic:
 * 1. Compute known ratio per difficulty level (K/P/F/C)
 * 2. Estimate = overall_known_ratio × cumulative_level_vocab
 *    where level is the highest level with actual data
 * 3. Cumulative vocab serves as a soft upper bound
 * 4. Acts as a lightweight correction to Algorithm 1
 *
 * Cumulative vocab by level:
 *   K:  8,000   (upper bound for primary-level knowledge)
 *   P: 16,000   (junior high)
 *   F: 28,000   (senior high)
 *   C: 40,000   (college/CET-6, matches TVY max)
 */
@Component
public class LevelCalibrationEstimator implements VocabEstimator {

    private static final Map<String, Integer> LEVEL_CAP = new LinkedHashMap<>();
    static {
        LEVEL_CAP.put("K", 8000);
        LEVEL_CAP.put("P", 16000);
        LEVEL_CAP.put("F", 28000);
        LEVEL_CAP.put("C", 40000);
    }

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }

        int total = wordResults.size();
        int known = 0, unknown = 0;
        Map<String, int[]> levelStats = new HashMap<>();
        for (String l : LEVEL_CAP.keySet()) levelStats.put(l, new int[]{0, 0});

        for (Map<String, Object> item : wordResults) {
            String difficulty = (String) item.getOrDefault("difficulty", "K");
            boolean isKnown = (Boolean) item.getOrDefault("known", false);
            if (!levelStats.containsKey(difficulty)) difficulty = "K";
            int[] s = levelStats.get(difficulty);
            s[1]++;
            if (isKnown) { s[0]++; known++; }
            else unknown++;
        }

        double overallRatio = total > 0 ? (double) known / total : 0;

        // Find highest level with data; use its cap
        String topLevel = "K";
        Map<String, Double> levelRatios = new HashMap<>();
        for (String l : LEVEL_CAP.keySet()) {
            int[] s = levelStats.get(l);
            double r = s[1] > 0 ? (double) s[0] / s[1] : overallRatio;
            levelRatios.put(l, r);
            if (s[1] > 0) topLevel = l; // highest level with data
        }

        int cap = LEVEL_CAP.get(topLevel);
        // Use weighted ratio across all levels, but cap at topLevel's cap
        double weightedRatio = 0;
        double totalWeight = 0;
        for (String l : LEVEL_CAP.keySet()) {
            double w = LEVEL_CAP.get(l);
            weightedRatio += levelRatios.get(l) * w;
            totalWeight += w;
        }
        weightedRatio = weightedRatio / totalWeight;

        int estimate = (int) Math.round(weightedRatio * cap);
        estimate = Math.max(0, Math.min(40000, estimate));

        // Range
        double sampleFactor = Math.min(1.0, total / 40.0);
        int rangeWidth = (int) (estimate * 0.3 * (1.4 - sampleFactor * 0.5));
        rangeWidth = Math.max(500, rangeWidth);
        int minRange = Math.max(0, estimate - rangeWidth / 2);
        int maxRange = Math.min(40000, estimate + rangeWidth / 2);

        // Confidence
        double sampleConf = Math.min(1.0, total / 50.0);
        double balanceConf = 1.0 - Math.abs(overallRatio - 0.5) * 1.2;
        balanceConf = Math.max(0.2, Math.min(1.0, balanceConf));
        double confidence = (sampleConf * 0.5 + balanceConf * 0.5) * 100;
        confidence = Math.max(5, Math.min(100, confidence));

        return new AlgorithmResult(estimate, minRange, maxRange, confidence, known, unknown, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Level-Capped Estimation";
    }
}
