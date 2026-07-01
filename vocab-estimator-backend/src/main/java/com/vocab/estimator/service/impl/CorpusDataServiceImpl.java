package com.vocab.estimator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocab.estimator.entity.CorpusData;
import com.vocab.estimator.entity.VocWord;
import com.vocab.estimator.mapper.CorpusDataMapper;
import com.vocab.estimator.service.CorpusDataService;
import com.vocab.estimator.service.VocWordService;
import com.vocab.estimator.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CorpusDataServiceImpl extends ServiceImpl<CorpusDataMapper, CorpusData> implements CorpusDataService {

    @Autowired private VocWordService vocWordService;
    private final ObjectMapper om = new ObjectMapper();
    private final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]{2,}");

    @Override
    public CorpusData importCorpusText(String corpusType, String rawText) {
        CorpusData data = new CorpusData();
        data.setCorpusType(corpusType.toUpperCase());
        data.setRawText(rawText);
        Set<String> uniqueWords = new HashSet<>();
        Matcher matcher = WORD_PATTERN.matcher(rawText);
        while (matcher.find()) uniqueWords.add(matcher.group().toLowerCase());
        try { data.setExtractedWords(om.writeValueAsString(new ArrayList<>(uniqueWords))); }
        catch (Exception e) { data.setExtractedWords("[]"); }
        save(data);
        return data;
    }

    @Override
    public CorpusAnalysisDTO analyzeCorpus(Long corpusId) {
        CorpusData data = getById(corpusId);
        if (data == null) return null;
        List<String> words;
        try { words = om.readValue(data.getExtractedWords(), new TypeReference<List<String>>(){}); }
        catch (Exception e) { words = new ArrayList<>(); }
        if (words.isEmpty()) {
            CorpusAnalysisDTO empty = new CorpusAnalysisDTO(data.getCorpusType(), 0, new HashMap<>(), 
                new EstimateResultDTO(0, 0, 0, 0.0, 0, 0, 0));
            return empty;
        }
        // Build word-level distribution - classify each word by difficulty
        Map<String, Integer> levelDist = new HashMap<>();
        levelDist.put("K",0); levelDist.put("P",0); levelDist.put("F",0); levelDist.put("C",0);
        
        for (String w : words) {
            VocWord vw = vocWordService.findByWord(w);
            String diff;
            if (vw != null) {
                diff = vw.getDifficulty();
            } else {
                // Estimate difficulty by word length (longer words = harder)
                if (w.length() <= 4) diff = "K";
                else if (w.length() <= 6) diff = "P";
                else if (w.length() <= 9) diff = "F";
                else diff = "C";
            }
            levelDist.merge(diff, 1, Integer::sum);
        }
        
        // Calculate estimated vocabulary based on difficulty distribution
        // Weighted average: higher level words -> higher vocabulary estimate
        // Base vocabulary per level: K=500, P=2000, F=5000, C=15000+
        Map<String, Integer> vocabBase = new HashMap<>();
        vocabBase.put("K", 500);
        vocabBase.put("P", 2000);
        vocabBase.put("F", 5000);
        vocabBase.put("C", 15000);
        
        int total = words.size();
        double weightedVocab = 0;
        double levelConfidence = 0;
        
        for (String level : new String[]{"K","P","F","C"}) {
            int count = levelDist.getOrDefault(level, 0);
            double ratio = total > 0 ? (double) count / total : 0;
            int base = vocabBase.getOrDefault(level, 500);
            // Weighted contribution: higher levels have higher vocabulary impact
            weightedVocab += ratio * base;
            // Track confidence based on data coverage
            if (count > 0) levelConfidence += ratio;
        }
        
        // The estimate is the weighted vocabulary level
        int estimate = (int) weightedVocab;
        // Range: +/- 30% for confidence interval
        int rangeWidth = (int)(estimate * 0.3);
        int minRange = Math.max(100, estimate - rangeWidth);
        int maxRange = estimate + rangeWidth;
        
        // Confidence: based on sample size and level distribution clarity
        double sampleConf = Math.min(1.0, total / 30.0);
        double distributionClarity = 1.0;
        // Check if most words concentrate in one level
        int maxLevel = levelDist.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (maxLevel > 0) {
            double dominantRatio = (double) maxLevel / total;
            distributionClarity = 0.3 + dominantRatio * 0.7; // higher if one level dominates
        }
        double confidence = (sampleConf * 0.5 + distributionClarity * 0.5) * 100;
        confidence = Math.min(100, Math.max(5, confidence));
        
        EstimateResultDTO est = new EstimateResultDTO(estimate, minRange, maxRange, 
            confidence, total, 0, total);
        CorpusAnalysisDTO dto = new CorpusAnalysisDTO(data.getCorpusType(), total, levelDist, est);
        try { data.setAnalysisResult(om.writeValueAsString(dto)); updateById(data); } catch (Exception e) {}
        return dto;
    }
    @Override
    public List<CorpusAnalysisDTO> analyzeAllCorpuses() {
        List<CorpusAnalysisDTO> results = new ArrayList<>();
        for (String type : new String[]{"C","F","P","K"}) {
            CorpusData data = baseMapper.findLatestByType(type);
            if (data != null) results.add(analyzeCorpus(data.getId()));
        }
        return results;
    }
}
