package com.vocab.estimator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vocab.estimator.entity.VocWord;
import com.vocab.estimator.mapper.VocWordMapper;
import com.vocab.estimator.service.VocWordService;
import com.vocab.estimator.dto.TestPaperDTO;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Vocabulary word service implementation
 */
@Service
public class VocWordServiceImpl extends ServiceImpl<VocWordMapper, VocWord> implements VocWordService {

    @Override
    public TestPaperDTO generateTestPaper(Long userId, int wordCount) {
        TestPaperDTO dto = new TestPaperDTO();
        dto.setUserId(userId);
        dto.setTestId(System.currentTimeMillis());
        
        List<Map<String, Object>> words = new ArrayList<>();
        String[] levels = {"K", "P", "F", "C"};
        int perLevel = Math.max(1, wordCount / 4);
        
        Random rand = new Random();
        for (String level : levels) {
            List<VocWord> levelWords = baseMapper.getRandomWordsByLevel(level, perLevel);
            for (VocWord w : levelWords) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", w.getId());
                item.put("word", w.getWord());
                item.put("difficulty", w.getDifficulty());
                item.put("definition", w.getDefinition());
                words.add(item);
            }
        }
        
        // Shuffle words
        Collections.shuffle(words, rand);
        dto.setWords(words);
        dto.setTotalCount(words.size());
        return dto;
    }

    @Override
    public VocWord findByWord(String word) {
        return lambdaQuery().eq(VocWord::getWord, word).one();
    }

    @Override
    public List<VocWord> searchWords(String keyword) {
        return baseMapper.searchWords(keyword);
    }

    @Override
    public int getWordCount() {
        return Math.toIntExact(lambdaQuery().count());
    }

    @Override
    public List<VocWord> getRandomWordsByLevel(String level, int count) {
        return baseMapper.getRandomWordsByLevel(level, count);
    }
}
