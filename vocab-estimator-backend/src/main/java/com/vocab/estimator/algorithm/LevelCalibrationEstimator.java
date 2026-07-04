package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 2: Lightweight Level Calibration
 *
 * Weighted ratio capped at the vocabulary level corresponding to
 * the highest difficulty level present in the test data.
 * Caps are modest (total 25000) to keep Di in TVY-observable range.
 *
 * Caps: K=5000, P=10000, F=18000, C=25000
 */
@Component
public class LevelCalibrationEstimator implements VocabEstimator {

    private static final Map<String, Integer> LEVEL_CAP = new LinkedHashMap<>();
    static {
        LEVEL_CAP.put("K", 5000);
        LEVEL_CAP.put("P", 10000);
        LEVEL_CAP.put("F", 18000);
        LEVEL_CAP.put("C", 25000);
    }

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }

        int total = wordResults.size();
        int known = 0, unknown = 0;
        Map<String, int[]> stats = new HashMap<>();
        for (String l : LEVEL_CAP.keySet()) stats.put(l, new int[]{0, 0});

        for (Map<String, Object> item : wordResults) {
            String diff = (String) item.getOrDefault("difficulty", "K");
            boolean ik = (Boolean) item.getOrDefault("known", false);
            if (!stats.containsKey(diff)) diff = "K";
            int[] s = stats.get(diff); s[1]++;
            if (ik) { s[0]++; known++; } else unknown++;
        }

        double overallRatio = total > 0 ? (double) known / total : 0;
        String topLevel = "K";
        for (String l : LEVEL_CAP.keySet()) {
            if (stats.get(l)[1] > 0) topLevel = l;
        }

        int cap = LEVEL_CAP.get(topLevel);
        double weightedRatio = 0;
        double totalW = 0;
        for (String l : LEVEL_CAP.keySet()) {
            int[] s = stats.get(l);
            double r = s[1] > 0 ? (double) s[0] / s[1] : overallRatio;
            double w = LEVEL_CAP.get(l);
            weightedRatio += r * w;
            totalW += w;
        }
        weightedRatio = totalW > 0 ? weightedRatio / totalW : 0;

        int estimate = (int) Math.round(weightedRatio * cap);
        estimate = Math.max(0, Math.min(40000, estimate));

        double sf = Math.min(1.0, total / 40.0);
        int rw = (int) (estimate * 0.3 * (1.4 - sf * 0.5));
        rw = Math.max(500, rw);
        int mn = Math.max(0, estimate - rw / 2);
        int mx = Math.min(40000, estimate + rw / 2);

        double sc = Math.min(1.0, total / 50.0);
        double bc = 1.0 - Math.abs(overallRatio - 0.5) * 1.2;
        bc = Math.max(0.2, Math.min(1.0, bc));
        double cf = (sc * 0.5 + bc * 0.5) * 100;
        cf = Math.max(5, Math.min(100, cf));

        return new AlgorithmResult(estimate, mn, mx, cf, known, unknown, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Level-Capped (25000)";
    }
}
