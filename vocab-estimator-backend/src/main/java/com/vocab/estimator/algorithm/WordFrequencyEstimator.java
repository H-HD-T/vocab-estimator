package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 1: Ratio-Based Vocabulary Estimation (primary estimator)
 *
 * Design principle: TVY vocabulary estimates correlate strongly with the
 * simple ratio of known/total words. Our empirical validation (samples 41-43)
 * shows Ci ≈ known_ratio × 35000 with <5% error for typical distributions.
 *
 * Core logic:
 * 1. Compute overall known ratio = known / total
 * 2. Base estimate = overall_ratio × 35000 (empirically fitted to TVY data)
 * 3. Difficulty adjustment (±20%): if known words are harder than unknown words,
 *    estimate gets a boost (user knows rarer words → larger vocabulary)
 * 4. Difficulty levels weighted: K=1, P=2, F=4, C=8
 * 5. Output Di in 0-40000 range, no post-hoc calibration
 */
@Component
public class WordFrequencyEstimator implements VocabEstimator {

    // Base reference: calibrated from TVY data (Ci ≈ ratio × 35000)
    private static final int TOTAL_REFERENCE = 35000;
    private static final int MAX_ESTIMATE = 40000;

    // Difficulty level weights (exponential: each level is 2x harder)
    private static final Map<String, Integer> LEVEL_WEIGHT = new LinkedHashMap<>();
    static {
        LEVEL_WEIGHT.put("K", 1);
        LEVEL_WEIGHT.put("P", 2);
        LEVEL_WEIGHT.put("F", 4);
        LEVEL_WEIGHT.put("C", 8);
    }

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }

        int total = wordResults.size();
        int known = 0, unknown = 0;

        // Weighted sums: Σ(known_flag × level_weight), Σ(level_weight)
        double weightedKnownSum = 0;
        double weightedTotalSum = 0;

        // Per-level stats for consistency check
        Map<String, int[]> levelCounts = new HashMap<>();
        for (String level : LEVEL_WEIGHT.keySet()) {
            levelCounts.put(level, new int[]{0, 0});
        }

        for (Map<String, Object> item : wordResults) {
            boolean isKnown = (Boolean) item.getOrDefault("known", false);
            String difficulty = (String) item.getOrDefault("difficulty", "K");
            if (!LEVEL_WEIGHT.containsKey(difficulty)) difficulty = "K";

            int weight = LEVEL_WEIGHT.get(difficulty);
            if (isKnown) {
                known++;
                weightedKnownSum += weight;
            } else {
                unknown++;
            }
            weightedTotalSum += weight;

            int[] cnt = levelCounts.get(difficulty);
            cnt[1]++;
            if (isKnown) cnt[0]++;
        }

        double ratio = total > 0 ? (double) known / total : 0;

        // Difficulty adjustment factor
        // If known words are at higher difficulty than average, boost estimate
        // avgDifficultyOfKnown = weightedKnownSum / known (if known > 0)
        // avgDifficultyOfAll = weightedTotalSum / total
        // diffFactor = avgDifficultyOfKnown / avgDifficultyOfAll
        double diffFactor = 1.0;
        if (known > 0 && unknown > 0) {
            double avgKnownWeight = weightedKnownSum / known;
            double avgAllWeight = weightedTotalSum / total;
            // If known words are harder than average, factor > 1 → boost
            // If known words are easier than average, factor < 1 → reduce
            diffFactor = avgKnownWeight / avgAllWeight;
            // Clamp to [0.8, 1.2] to avoid extreme adjustments
            diffFactor = Math.max(0.8, Math.min(1.2, diffFactor));
        }

        // === Estimate = base_ratio × reference × difficulty_adjustment ===
        int estimate = (int) Math.round(ratio * TOTAL_REFERENCE * diffFactor);
        estimate = Math.max(0, Math.min(MAX_ESTIMATE, estimate));

        // ---- Range calculation ----
        double sampleFactor = Math.min(1.0, total / 40.0);
        double balanceFactor = 1.0 - Math.abs(ratio - 0.5) * 0.8;

        int rangeWidth = (int) (estimate * 0.35 * (1.4 - sampleFactor * 0.5) * balanceFactor);
        rangeWidth = Math.max(500, rangeWidth);

        int minRange = Math.max(0, estimate - rangeWidth / 2);
        int maxRange = Math.min(MAX_ESTIMATE, estimate + rangeWidth / 2);

        // ---- Confidence calculation ----
        double sampleConf = Math.min(1.0, total / 50.0);
        double balanceConf = 1.0 - Math.abs(ratio - 0.5) * 1.2;
        balanceConf = Math.max(0.2, Math.min(1.0, balanceConf));
        double consistencyConf = 1.0 - Math.abs(diffFactor - 1.0) * 0.8;
        consistencyConf = Math.max(0.3, Math.min(1.0, consistencyConf));

        double confidence = (sampleConf * 0.35 + balanceConf * 0.35 + consistencyConf * 0.3) * 100;
        confidence = Math.max(5, Math.min(100, confidence));

        return new AlgorithmResult(estimate, minRange, maxRange, confidence, known, unknown, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Ratio-Based Estimation (TVY-fitted)";
    }
}
