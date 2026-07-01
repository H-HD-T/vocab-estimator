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
        List<BatchResultDTO.BatchItemResult> results = new ArrayList<>();
        for (String line : wordLines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("[,;]");
            String word = parts[0].trim();
            boolean known = parts.length > 1 && (parts[1].trim().toLowerCase().contains("known") ||
                parts[1].trim().toLowerCase().contains("yes") || parts[1].trim().contains("true") ||
                parts[1].trim().contains("1") || parts[1].trim().contains("reco"));
            VocWord vw = vocWordService.findByWord(word);
            List<Map<String, Object>> wr = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("word", word);
            item.put("known", known);
            item.put("difficulty", vw != null ? vw.getDifficulty() : "K");
            item.put("frequency", vw != null ? vw.getFrequency() : 0.5);
            wr.add(item);
            AlgorithmResult ar = algorithmFactory.estimateAll(wr);
            EstimateResultDTO dto = new EstimateResultDTO(ar.getEstimate(), ar.getMinRange(), ar.getMaxRange(),
                ar.getConfidence(), ar.getKnownCount(), ar.getUnknownCount(), ar.getTotalWords());
            results.add(new BatchResultDTO.BatchItemResult(line, dto));
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
    public List<BatchTask> getTaskHistory() {
        return baseMapper.findAllOrderByTime();
    }
}
