package com.vocab.estimator.algorithm;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm 1: Word Frequency Weighted Estimation
 * 
 * Core logic:
 * 1. Map known/unknown ratio to an estimated vocabulary base
 * 2. Weight each word by its frequency: low-freq known words contribute more
 * 3. Use frequency distribution to compute confidence interval
 * 4. Confidence is derived from sample size and frequency distribution balance
 * 
 * Reference vocabulary sizes by level:
 *   K (Primary): ~500 words
 *   P (Junior): ~2000 words
 *   F (Senior): ~4500 words  
 *   C (College+): ~8000 words
 *   Total estimated: ~15000+ words
 */
@Component
public class WordFrequencyEstimator implements VocabEstimator {

    // Base vocabulary sizes per difficulty level
    private static final Map<String, Integer> LEVEL_BASE_VOCAB = new HashMap<>();
    static {
        LEVEL_BASE_VOCAB.put("K", 600);
        LEVEL_BASE_VOCAB.put("P", 2000);
        LEVEL_BASE_VOCAB.put("F", 4000);
        LEVEL_BASE_VOCAB.put("C", 6000);
    }

    private static final int TOTAL_REFERENCE_VOCAB = 20000;

    @Override
    public AlgorithmResult estimate(List<Map<String, Object>> wordResults) {
        if (wordResults == null || wordResults.isEmpty()) {
            return new AlgorithmResult(0, 0, 0, 0, 0, 0, 0);
        }
        
        int known = 0;
        int unknown = 0;
        double weightedScore = 0;
        double totalWeight = 0;
        
        // Track per-level stats for calibration
        Map<String, Integer> levelKnown = new HashMap<>();
        Map<String, Integer> levelTotal = new HashMap<>();
        
        for (Map<String, Object> item : wordResults) {
            boolean isKnown = (Boolean) item.getOrDefault("known", false);
            String difficulty = (String) item.getOrDefault("difficulty", "K");
            Double frequency = (Double) item.getOrDefault("frequency", 0.5);
            
            // Track per-level
            levelTotal.merge(difficulty, 1, Integer::sum);
            if (isKnown) {
                known++;
                levelKnown.merge(difficulty, 1, Integer::sum);
            } else {
                unknown++;
            }
            
            // Weight: lower frequency words that are known contribute more
            // High-frequency known words contribute less (too easy)
            double wordWeight = isKnown ? (1.0 - frequency + 0.1) : -(frequency + 0.1);
            weightedScore += wordWeight;
            totalWeight += (isKnown ? (1.0 - frequency + 0.1) : (frequency + 0.1));
        }
        
        int total = wordResults.size();
        double knownRatio = total > 0 ? (double) known / total : 0;
        
        // ---- Core estimation logic ----
        // 1. Base estimate from known ratio alone
        double baseEstimate = knownRatio * TOTAL_REFERENCE_VOCAB;
        
        // 2. Frequency-weighted adjustment
        // If user knows low-frequency words, estimated vocab increases
        double freqAdjustment = totalWeight > 0 ? (weightedScore / totalWeight) * 2000 : 0;
        
        // 3. Level-based adjustment
        double levelAdjustment = 0;
        for (String level : LEVEL_BASE_VOCAB.keySet()) {
            int lt = levelTotal.getOrDefault(level, 0);
            int lk = levelKnown.getOrDefault(level, 0);
            if (lt > 0) {
                double lr = (double) lk / lt;
                int baseVocab = LEVEL_BASE_VOCAB.get(level);
                // Known ratio at higher levels contributes more
                int levelMultiplier = level.equals("C") ? 4 : level.equals("F") ? 3 : level.equals("P") ? 2 : 1;
                levelAdjustment += lr * baseVocab * levelMultiplier * 0.3;
            }
        }
        
        int estimate = (int) Math.round(baseEstimate + freqAdjustment + levelAdjustment);
        estimate = Math.max(0, Math.min(estimate, TOTAL_REFERENCE_VOCAB * 2));
        
        // ---- Range calculation ----
        // Range widens with fewer samples or extreme ratios
        double sampleFactor = Math.min(1.0, total / 50.0);
        double ratioSpread = 1.0 - Math.abs(knownRatio - 0.5) * 2;  // max at 50/50 split
        int rangeWidth = (int) ((1.0 - sampleFactor * 0.5) * 3000 + (1.0 - ratioSpread) * 2000);
        
        int minRange = Math.max(0, estimate - rangeWidth / 2);
        int maxRange = estimate + rangeWidth / 2;
        
        // ---- Confidence calculation ----
        // Confidence increases with sample size and decreases with extreme ratios
        double sampleConf = Math.min(1.0, total / 100.0);  // more samples = higher confidence
        double balanceConf = 1.0 - Math.abs(knownRatio - 0.5) * 1.5;  // 50/50 gives best confidence
        balanceConf = Math.max(0.1, balanceConf);
        double confidence = (sampleConf * 0.6 + balanceConf * 0.4) * 100;
        confidence = Math.min(100, Math.max(0, confidence));
        
        return new AlgorithmResult(estimate, minRange, maxRange, confidence, known, unknown, total);
    }

    @Override
    public String getAlgorithmName() {
        return "Word Frequency Weighted Estimation";
    }
}
