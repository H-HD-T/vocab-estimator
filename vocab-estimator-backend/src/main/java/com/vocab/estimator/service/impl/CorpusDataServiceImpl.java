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
        Map<String, Integer> levelDist = new HashMap<>();
        levelDist.put("K",0); levelDist.put("P",0); levelDist.put("F",0); levelDist.put("C",0);
        List<Map<String, Object>> wordResults = new ArrayList<>();
        for (String w : words) {
            VocWord vw = vocWordService.findByWord(w);
            String diff = vw != null ? vw.getDifficulty() : "K";
            double freq = vw != null ? vw.getFrequency() : 0.3;
            if (vw != null) levelDist.merge(diff, 1, Integer::sum);
            Map<String, Object> item = new HashMap<>();
            item.put("word", w); item.put("known", vw != null);
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
