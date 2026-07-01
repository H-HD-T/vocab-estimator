package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Algorithm 2: Hierarchical Difficulty Calibration
 * 
 * Core logic:
 * 1. Distinguish K/P/F/C four-level vocabulary difficulty
 * 2. Higher-level word recognition rate is weighted more heavily
 * 3. Validation: high-vocab users must have >=90% recognition on low-level words
 * 4. Low-vocab users have no mandatory threshold for high-level words
 * 5. Cross-calibrate using difficulty distribution to refine estimate
 * 
 * The algorithm assumes that vocabulary knowledge is hierarchical:
 * - A C-level user should know nearly all K/P/F words
 * - An F-level user should know nearly all K/P words
 * - A P-level user should know nearly all K words
 */
@Component
public class LevelCalibrationEstimator implements VocabEstimator {

    // Cumulative vocabulary per level (hierarchical)
    private static final Map<String, Integer> CUMULATIVE_VOCAB = new LinkedHashMap<>();
    static {
        CUMULATIVE_VOCAB.put("K", 2000);
        CUMULATIVE_VOCAB.put("P", 5000);
        CUMULATIVE_VOCAB.put("F", 10000);
        CUMULATIVE_VOCAB.put("C", 20000);
    }
    
    private static final int MAX_VOCAB = 45000;

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

        int known = 0;
        int unknown = 0;

        for (Map<String, Object> item : wordResults) {
            String difficulty = (String) item.getOrDefault("difficulty", "K");
            boolean isKnown = (Boolean) item.getOrDefault("known", false);
            
            if (!levelResults.containsKey(difficulty)) {
                difficulty = "K";
            }
            levelResults.get(difficulty).add(isKnown);
            
            if (isKnown) known++;
            else unknown++;
        }

        int total = wordResults.size();
        
        // ---- Calculate per-level recognition rates ----
        Map<String, Double> levelRates = new HashMap<>();
        for (String level : CUMULATIVE_VOCAB.keySet()) {
            List<Boolean> results = levelResults.get(level);
            if (results.isEmpty()) {
                levelRates.put(level, 0.5);  // default if no data
            } else {
                long knownCount = results.stream().filter(r -> r).count();
                levelRates.put(level, (double) knownCount / results.size());
            }
        }

        // ---- Calibration validation: high level users must know low level words ----
        // Determine candidate level based on highest level with >= 60% recognition
        String candidateLevel = "K";
        for (String level : CUMULATIVE_VOCAB.keySet()) {
            if (levelRates.getOrDefault(level, 0.0) >= 0.6) {
                candidateLevel = level;
            }
        }

        // Validation: if candidate is C or F, check lower level recognition >= 90%
        boolean validCalibration = true;
        if (candidateLevel.equals("C") || candidateLevel.equals("F")) {
            for (String level : CUMULATIVE_VOCAB.keySet()) {
                if (level.equals(candidateLevel)) break;
                if (levelRates.getOrDefault(level, 0.0) < 0.9) {
                    validCalibration = false;
                    // Downgrade candidate
                    candidateLevel = level;
                    break;
                }
            }
        }

        // ---- Hierarchical estimation ----
        // For each level, estimate the proportion of vocabulary mastered
        double masteredProportion = 0;
        int levelCount = 0;
        
        boolean reachedCandidate = false;
        for (String level : CUMULATIVE_VOCAB.keySet()) {
            double rate = levelRates.get(level);
            if (level.equals(candidateLevel)) {
                reachedCandidate = true;
            }
            // Weight: higher levels contribute more
            double levelWeight = level.equals("C") ? 4.0 : level.equals("F") ? 3.0 : level.equals("P") ? 2.0 : 1.0;
            
            if (reachedCandidate) {
                // At or above candidate level, use actual recognition
                masteredProportion += rate * levelWeight;
            } else {
                // Below candidate level, assume high recognition if calibration valid
                masteredProportion += Math.max(rate, 0.9) * levelWeight;
            }
            levelCount += levelWeight;
        }

        double avgMastery = levelCount > 0 ? masteredProportion / levelCount : 0;
        
        // Map mastery to vocabulary size
        int estimate = (int) (avgMastery * MAX_VOCAB);
        estimate = Math.max(0, Math.min(estimate, MAX_VOCAB));

        // ---- Range calculation ----
        int levelIndex = new ArrayList<>(CUMULATIVE_VOCAB.keySet()).indexOf(candidateLevel);
        int baseVocab = CUMULATIVE_VOCAB.getOrDefault(candidateLevel, 500);
        int nextVocab = levelIndex < CUMULATIVE_VOCAB.size() - 1 ? 
            new ArrayList<>(CUMULATIVE_VOCAB.values()).get(levelIndex + 1) : MAX_VOCAB;
        
        int minRange = Math.max(0, (int)(baseVocab * 0.7));
        int maxRange = Math.min(MAX_VOCAB, (int)(nextVocab * 1.3));

        // ---- Confidence ----
        double sampleConf = Math.min(1.0, total / 80.0);
        double calibrationConf = validCalibration ? 0.3 : 0;
        double levelConf = 1.0 - (1.0 - avgMastery) * 0.5;
        double confidence = (sampleConf * 0.4 + calibrationConf * 0.3 + levelConf * 0.3) * 100;
        confidence = Math.min(100, Math.max(0, confidence));

        return new AlgorithmResult(estimate, minRange, maxRange, confidence, known, unknown, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Hierarchical Level Calibration";
    }
}
