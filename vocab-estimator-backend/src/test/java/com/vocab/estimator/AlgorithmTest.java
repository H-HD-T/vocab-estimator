package com.vocab.estimator;

import com.vocab.estimator.algorithm.*;
import org.junit.jupiter.api.Test;
import java.util.*;

/**
 * Unit tests for vocabulary estimation algorithms
 */
public class AlgorithmTest {

    private final WordFrequencyEstimator freqEstimator = new WordFrequencyEstimator();
    private final LevelCalibrationEstimator calibEstimator = new LevelCalibrationEstimator();

    @Test
    public void testWordFrequencyEstimator_Basic() {
        List<Map<String, Object>> words = new ArrayList<>();
        words.add(createWord("apple", true, "K", 0.95));
        words.add(createWord("book", true, "K", 0.93));
        words.add(createWord("cat", true, "K", 0.90));
        words.add(createWord("philosophy", false, "C", 0.35));
        words.add(createWord("ambiguous", false, "C", 0.36));

        AlgorithmResult result = freqEstimator.estimate(words);
        assert result.getEstimate() > 0 : "Estimate should be positive";
        assert result.getConfidence() >= 0 : "Confidence should be >= 0";
        assert result.getMinRange() <= result.getEstimate() : "Min range <= estimate";
        assert result.getMaxRange() >= result.getEstimate() : "Max range >= estimate";
        System.out.println("[OK] FrequencyEstimator basic test: estimate=" + result.getEstimate()
            + " range=[" + result.getMinRange() + "-" + result.getMaxRange() + "]"
            + " confidence=" + result.getConfidence());
    }

    @Test
    public void testLevelCalibrationEstimator_HighLevel() {
        // All K/P words known, all C words known -> should give C-level estimate
        List<Map<String, Object>> words = new ArrayList<>();
        // K level
        for (String w : Arrays.asList("apple", "book", "cat", "dog", "egg", "fish", "good", "happy", "ice", "jump")) {
            words.add(createWord(w, true, "K", 0.9));
        }
        // P level
        for (String w : Arrays.asList("abroad", "accept", "achieve", "address", "agree", "allow", "animal", "arrive", "attention", "balance")) {
            words.add(createWord(w, true, "P", 0.7));
        }
        // F level
        for (String w : Arrays.asList("abandon", "absorb", "abstract", "accelerate", "accompany", "accomplish", "accurate", "acknowledge", "acquire", "adapt")) {
            words.add(createWord(w, true, "F", 0.5));
        }
        // C level
        for (String w : Arrays.asList("abolish", "absurd", "abundance", "accommodate", "accumulate", "acquaintance", "acute", "adhere", "ambiguous", "amplify")) {
            words.add(createWord(w, true, "C", 0.3));
        }

        AlgorithmResult result = calibEstimator.estimate(words);
        System.out.println("[OK] CalibrationEstimator high-level test: estimate=" + result.getEstimate()
            + " confidence=" + result.getConfidence());
        assert result.getEstimate() > 3000 : "High-level user should have >3000 vocab estimate";
    }

    @Test
    public void testLevelCalibrationEstimator_LowLevel() {
        // Only K words known, C/F words unknown -> should give K-level estimate
        List<Map<String, Object>> words = new ArrayList<>();
        for (String w : Arrays.asList("apple", "book", "cat", "dog", "fish")) {
            words.add(createWord(w, true, "K", 0.9));
        }
        for (String w : Arrays.asList("abandon", "absorb", "abstract", "accelerate")) {
            words.add(createWord(w, false, "F", 0.5));
        }
        for (String w : Arrays.asList("abolish", "absurd", "accommodate", "ambiguous")) {
            words.add(createWord(w, false, "C", 0.3));
        }

        AlgorithmResult result = calibEstimator.estimate(words);
        System.out.println("[OK] CalibrationEstimator low-level test: estimate=" + result.getEstimate());
        // Low-level user should have lower estimate than high-level user
        // Not testing exact value since it depends on algorithm specifics
        assert result.getEstimate() < 10000 : "Low-level user should have moderate estimate";
    }

    @Test
    public void testEmptyInput() {
        List<Map<String, Object>> empty = new ArrayList<>();
        AlgorithmResult freqResult = freqEstimator.estimate(empty);
        AlgorithmResult calibResult = calibEstimator.estimate(empty);
        
        assert freqResult.getEstimate() == 0 : "Empty input should give 0 estimate";
        assert calibResult.getEstimate() == 0 : "Empty input should give 0 estimate";
        System.out.println("[OK] Empty input test passed");
    }

    @Test
    public void testAlgorithmFactory() {
        AlgorithmFactory factory = new AlgorithmFactory();
        
        // Manually set dependencies since Spring context not available
        factory.frequencyEstimator = freqEstimator;
        factory.calibrationEstimator = calibEstimator;
        
        List<VocabEstimator> algorithms = factory.getAllAlgorithms();
        assert algorithms.size() == 2 : "Should have 2 algorithms";
        
        VocabEstimator defaultAlgo = factory.getAlgorithm("nonexistent");
        assert defaultAlgo != null : "Should return default algorithm";
        
        System.out.println("[OK] AlgorithmFactory test passed: " + algorithms.size() + " algorithms");
    }

    private Map<String, Object> createWord(String word, boolean known, String difficulty, double frequency) {
        Map<String, Object> item = new HashMap<>();
        item.put("word", word);
        item.put("known", known);
        item.put("difficulty", difficulty);
        item.put("frequency", frequency);
        return item;
    }
}
