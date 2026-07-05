package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 1: Difficulty-Weighted Vocabulary Estimation (primary)
 *
 * Formula:
 *   nr = Sum(r_l x w_l) / Sum(w_l)  where r_l = level recognition rate, w_l = level cap
 *   Di = nr x cap_C
 *
 *  * Per-level DB word counts: K=208, P=1800, F=1925, C=3644
 */
@Component
public class WordFrequencyEstimator implements VocabEstimator {

    private static final Map<String, Integer> LEVEL_CAP = new LinkedHashMap<>();
    static {
        LEVEL_CAP.put("K", 2000);
        LEVEL_CAP.put("P", 7000);
        LEVEL_CAP.put("F", 20000);
        LEVEL_CAP.put("C", 40000);
    }

    private static final int MAX_ESTIMATE = 40000;

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }

        int total = wordResults.size();
        int known = 0;
        Map<String, int[]> stats = new HashMap<>();
        for (String l : LEVEL_CAP.keySet()) stats.put(l, new int[]{0, 0});

        for (Map<String, Object> item : wordResults) {
            String diff = (String) item.getOrDefault("difficulty", "K");
            boolean ik = (Boolean) item.getOrDefault("known", false);
            if (!stats.containsKey(diff)) diff = "K";
            int[] s = stats.get(diff); s[1]++;
            if (ik) { s[0]++; known++; }
        }

                double overallRatio = total > 0 ? (double) known / total : 0;

        // Power-law formula: Di = MAX_ESTIMATE x (known/total)^1.5
        // Compresses scale: 50% known -> 28% of max, 25% known -> 12.5% of max
        int estimate = (int) Math.round(MAX_ESTIMATE * Math.pow(overallRatio, 1.5));
        estimate = Math.max(100, Math.min(MAX_ESTIMATE, estimate));

        double sampleFactor = Math.min(1.0, total / 40.0);
        int rangeWidth = (int) (estimate * 0.35 * (1.5 - sampleFactor * 0.6));
        rangeWidth = Math.max(500, rangeWidth);
        int minRange = Math.max(0, estimate - rangeWidth / 2);
        int maxRange = Math.min(MAX_ESTIMATE, estimate + rangeWidth / 2);

        double sampleConf = Math.min(1.0, total / 50.0);
        double balanceConf = 1.0 - Math.abs(overallRatio - 0.5) * 1.2;
        balanceConf = Math.max(0.3, Math.min(1.0, balanceConf));
        double confidence = (sampleConf * 0.5 + balanceConf * 0.5) * 100;
        confidence = Math.max(5, Math.min(100, confidence));

        return new AlgorithmResult(estimate, minRange, maxRange, confidence, known, total - known, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Per-Level Vocab Sum (K2k+P5k+F13k+C20k)";
    }
}

