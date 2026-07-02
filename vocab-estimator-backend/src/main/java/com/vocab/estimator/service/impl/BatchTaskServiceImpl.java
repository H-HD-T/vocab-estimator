package com.vocab.estimator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vocab.estimator.entity.BatchTask;
import com.vocab.estimator.entity.VocWord;
import com.vocab.estimator.mapper.BatchTaskMapper;
import com.vocab.estimator.service.BatchTaskService;
import com.vocab.estimator.service.VocWordService;
import com.vocab.estimator.algorithm.AlgorithmFactory;
import com.vocab.estimator.algorithm.AlgorithmResult;
import com.vocab.estimator.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class BatchTaskServiceImpl extends ServiceImpl<BatchTaskMapper, BatchTask> implements BatchTaskService {

    @Autowired private VocWordService vocWordService;
    @Autowired private AlgorithmFactory algorithmFactory;
    private final Random random = new Random();

    @Override
    public BatchResultDTO processBatchWords(List<String> wordLines) {
        // Process ALL words together as one vocabulary test
        List<BatchResultDTO.BatchItemResult> results = new ArrayList<>();
        List<Map<String, Object>> allWordResults = new ArrayList<>();
        int totalKnown = 0, totalUnknown = 0;
        
        for (String line : wordLines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("[,;]");
            String word = parts[0].trim();
            boolean known = false;
            if (parts.length > 1) {
                String tag = parts[1].trim().toLowerCase();
                known = tag.equals("known") || tag.equals("yes") || tag.equals("true") || tag.equals("1")
                    || tag.equals("recognized") || tag.contains("known,")
                    || tag.startsWith("known") || tag.startsWith("yes") || tag.startsWith("recognized");
            }
            VocWord vw = vocWordService.findByWord(word);
            String difficulty = vw != null ? vw.getDifficulty() : guessDifficulty(word);
            double freq = vw != null ? vw.getFrequency() : Math.min(0.8, 5.0 / Math.max(word.length(), 2));
            
            Map<String, Object> item = new HashMap<>();
            item.put("word", word);
            item.put("known", known);
            item.put("difficulty", difficulty);
            item.put("frequency", freq);
            allWordResults.add(item);
            if (known) totalKnown++; else totalUnknown++;
        }
        
        // Run algorithm ONCE on ALL words combined
        AlgorithmResult ar = algorithmFactory.estimateAll(allWordResults);
        EstimateResultDTO combinedDto = new EstimateResultDTO(ar.getEstimate(), ar.getMinRange(), ar.getMaxRange(),
            ar.getConfidence(), ar.getKnownCount(), ar.getUnknownCount(), ar.getTotalWords());
        
        // Also create per-word items with algorithm info for display
        for (String line : wordLines) {
            if (line.trim().isEmpty()) continue;
            results.add(new BatchResultDTO.BatchItemResult(line, combinedDto));
        }
        return new BatchResultDTO(results, String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public BatchResultDTO processBatchText(String textContent) {
        return processBatchWords(Arrays.asList(textContent.split("[\\n\\r]+")));
    }

    @Override
    public SamplingResultDTO runSamplingTest(int sampleLength, int knowRatio) {
        List<Integer> allEstimates = new ArrayList<>();
        for (int i = 0; i < 900; i++) {
            List<Map<String, Object>> wordResults = new ArrayList<>();
            for (String level : new String[]{"K","P","F","C"}) {
                List<VocWord> levelWords = vocWordService.getRandomWordsByLevel(level, sampleLength / 4);
                for (VocWord w : levelWords) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("word", w.getWord());
                    item.put("known", random.nextInt(100) < knowRatio);
                    item.put("difficulty", w.getDifficulty());
                    item.put("frequency", w.getFrequency());
                    wordResults.add(item);
                }
            }
            allEstimates.add(algorithmFactory.estimateAll(wordResults).getEstimate());
        }
        double mean = allEstimates.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = allEstimates.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        SamplingResultDTO dto = new SamplingResultDTO();
        dto.setSampleLength(sampleLength);
        dto.setKnowRatio(knowRatio);
        dto.setSampleCount(900);
        dto.setMeanEstimate(Math.round(mean * 100.0) / 100.0);
        dto.setVariance(Math.round(variance * 100.0) / 100.0);
        dto.setAllEstimates(allEstimates);
        return dto;
    }

    @Override
    public StabilityResultDTO runStabilityTest() {
        // Use ALL voc_word as vocabulary list A for stability testing
        List<VocWord> allWords = vocWordService.list();
        if (allWords.size() < 20) {
            return new StabilityResultDTO(new ArrayList<>());
        }
        
        int[] ratios = {10, 20, 30};         // know ratio percentages
        int[] lengths = {200, 300, 400};     // test list lengths  
        int runsPerCombo = 100;              // repeat 100 times per combo
        
        List<StabilityResultDTO.StabilityComboResult> combos = new ArrayList<>();
        
        int totalAvailable = allWords.size();
        
        for (int ratio : ratios) {
            for (int len : lengths) {
                List<Integer> estimates = new ArrayList<>();
                
                for (int run = 0; run < runsPerCombo; run++) {
                    List<Map<String, Object>> wordResults = new ArrayList<>();
                    
                    // Sample with replacement if len > totalAvailable
                    for (int i = 0; i < len; i++) {
                        VocWord w = allWords.get(random.nextInt(totalAvailable));
                        Map<String, Object> item = new HashMap<>();
                        item.put("word", w.getWord());
                        item.put("known", random.nextInt(100) < ratio);
                        item.put("difficulty", w.getDifficulty());
                        item.put("frequency", w.getFrequency());
                        wordResults.add(item);
                    }
                    
                    int est = algorithmFactory.estimateAll(wordResults).getEstimate();
                    estimates.add(est);
                }
                
                double mean = estimates.stream().mapToInt(Integer::intValue).average().orElse(0);
                double variance = estimates.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
                
                combos.add(new StabilityResultDTO.StabilityComboResult(
                    ratio, len, runsPerCombo,
                    Math.round(mean * 100.0) / 100.0,
                    Math.round(variance * 100.0) / 100.0,
                    estimates
                ));
            }
        }
        
        return new StabilityResultDTO(combos);
    }
    public List<BatchTask> getTaskHistory() {
        return baseMapper.findAllOrderByTime();
    }

    private String guessDifficulty(String word) {
        int len = word.length();
        if (len <= 4) return "K";
        else if (len <= 6) return "P";
        else if (len <= 9) return "F";
        else return "C";
    }
}
