package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 2: Same formula as primary (backup)
 */
@Component
public class LevelCalibrationEstimator implements VocabEstimator {

    private static final Map<String, Integer> LEVEL_CAP = new LinkedHashMap<>();
    static {
        LEVEL_CAP.put("K", 2000);
        LEVEL_CAP.put("P", 7000);
        LEVEL_CAP.put("F", 20000);
        LEVEL_CAP.put("C", 40000);
    }

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

        // Power-law formula: Di = 40000 x (known/total)^1.5
        // Compresses scale: 50% known -> 28% of max, 25% known -> 12.5% of max
        int estimate = (int) Math.round(40000 * Math.pow(overallRatio, 1.5));
        estimate = Math.max(100, Math.min(40000, estimate));
        estimate = Math.max(100, Math.min(estimate, 8000));

        double sf = Math.min(1.0, total / 40.0);
        int rw = (int) (estimate * 0.3 * (1.5 - sf * 0.6));
        rw = Math.max(500, rw);
        int mn = Math.max(0, estimate - rw / 2);
        int mx = Math.min(40000, estimate + rw / 2);

        double sc = Math.min(1.0, total / 50.0);
        double bc = 1.0 - Math.abs(overallRatio - 0.5) * 1.2;
        bc = Math.max(0.3, Math.min(1.0, bc));
        double cf = (sc * 0.5 + bc * 0.5) * 100;
        cf = Math.max(5, Math.min(100, cf));

        return new AlgorithmResult(estimate, mn, mx, cf, known, total - known, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Per-Level Vocab Sum (K2k+P5k+F13k+C20k)";
    }
}

