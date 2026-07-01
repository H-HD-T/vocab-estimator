package com.vocab.estimator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocab.estimator.entity.CorpusData;
import com.vocab.estimator.entity.VocWord;
import com.vocab.estimator.mapper.CorpusDataMapper;
import com.vocab.estimator.service.CorpusDataService;
import com.vocab.estimator.service.VocWordService;
import com.vocab.estimator.algorithm.AlgorithmFactory;
import com.vocab.estimator.algorithm.AlgorithmResult;
import com.vocab.estimator.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CorpusDataServiceImpl extends ServiceImpl<CorpusDataMapper, CorpusData> implements CorpusDataService {

    @Autowired private VocWordService vocWordService;
    @Autowired private AlgorithmFactory algorithmFactory;
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
                new EstimateResultDTO(0, 0, 0, 0, 0, 0, 0));
            return empty;
        }
        // Build word-level distribution and known/unknown for algorithm
        Map<String, Integer> levelDist = new HashMap<>();
        levelDist.put("K",0); levelDist.put("P",0); levelDist.put("F",0); levelDist.put("C",0);
        List<Map<String, Object>> wordResults = new ArrayList<>();
        int foundInDb = 0;
        for (String w : words) {
            VocWord vw = vocWordService.findByWord(w);
            String diff;
            double freq;
            boolean known;
            if (vw != null) {
                diff = vw.getDifficulty();
                freq = vw.getFrequency();
                known = true;
                levelDist.merge(diff, 1, Integer::sum);
                foundInDb++;
            } else {
                // Estimate difficulty by word length (longer words = harder)
                if (w.length() <= 4) diff = "K";
                else if (w.length() <= 6) diff = "P";
                else if (w.length() <= 9) diff = "F";
                else diff = "C";
                freq = Math.min(0.8, 5.0 / w.length());
                // Mark as known if the word is a reasonable English word (>2 letters, contains vowel)
                // This is a text analysis, not a test - all extracted words are "known" by the author
                known = w.matches(".*[aeiouy].*");
                levelDist.merge(diff, 1, Integer::sum);
            }
            Map<String, Object> item = new HashMap<>();
            item.put("word", w); item.put("known", known);
            item.put("difficulty", diff); item.put("frequency", freq);
            wordResults.add(item);
        }
        AlgorithmResult ar = algorithmFactory.estimateAll(wordResults);
        EstimateResultDTO est = new EstimateResultDTO(ar.getEstimate(), ar.getMinRange(), ar.getMaxRange(),
            ar.getConfidence(), ar.getKnownCount(), ar.getUnknownCount(), words.size());
        CorpusAnalysisDTO dto = new CorpusAnalysisDTO(data.getCorpusType(), words.size(), levelDist, est);
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
