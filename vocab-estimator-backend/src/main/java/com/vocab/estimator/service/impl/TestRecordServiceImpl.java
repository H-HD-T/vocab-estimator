package com.vocab.estimator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocab.estimator.entity.TestRecord;
import com.vocab.estimator.entity.VocWord;
import com.vocab.estimator.mapper.TestRecordMapper;
import com.vocab.estimator.service.TestRecordService;
import com.vocab.estimator.service.VocWordService;
import com.vocab.estimator.algorithm.AlgorithmFactory;
import com.vocab.estimator.algorithm.AlgorithmResult;
import com.vocab.estimator.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class TestRecordServiceImpl extends ServiceImpl<TestRecordMapper, TestRecord> implements TestRecordService {

    @Autowired private VocWordService vocWordService;
    @Autowired private AlgorithmFactory algorithmFactory;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public TestPaperDTO generateTestPaper(Long userId, int wordCount) {
        return vocWordService.generateTestPaper(userId, wordCount);
    }

    @Override
    public EstimateResultDTO submitTest(Long userId, TestSubmitDTO submitDTO) {
        List<Map<String, Object>> wordResults = new ArrayList<>();
        for (Map<String, Object> answer : submitDTO.getAnswers()) {
            String word = (String) answer.get("word");
            Object wid = answer.get("wordId");
            Boolean known = (Boolean) answer.get("known");
            VocWord vw = wid != null ? vocWordService.getById(Long.valueOf(wid.toString())) : vocWordService.findByWord(word);
            Map<String, Object> item = new HashMap<>();
            item.put("word", word);
            item.put("known", known != null && known);
            item.put("difficulty", vw != null ? vw.getDifficulty() : "K");
            item.put("frequency", vw != null ? vw.getFrequency() : 0.5);
            wordResults.add(item);
        }
        AlgorithmResult ar = algorithmFactory.estimateAll(wordResults);
        TestRecord record = new TestRecord();
        record.setUserId(userId);
        try { record.setTestWords(om.writeValueAsString(submitDTO.getAnswers())); } catch (Exception e) { record.setTestWords(""); }
        record.setKnownCount(ar.getKnownCount());
        record.setUnknownCount(ar.getUnknownCount());
        record.setEstimateVocab(ar.getEstimate());
        record.setMinRange(ar.getMinRange());
        record.setMaxRange(ar.getMaxRange());
        record.setConfidence(ar.getConfidence());
        record.setTestType(submitDTO.getTestType() != null ? submitDTO.getTestType() : "GUI");
        save(record);
        return new EstimateResultDTO(ar.getEstimate(), ar.getMinRange(), ar.getMaxRange(),
            ar.getConfidence(), ar.getKnownCount(), ar.getUnknownCount(), ar.getTotalWords());
    }

    @Override
    public List<TestRecord> getUserTestHistory(Long userId) {
        return baseMapper.findByUserId(userId);
    }
}
