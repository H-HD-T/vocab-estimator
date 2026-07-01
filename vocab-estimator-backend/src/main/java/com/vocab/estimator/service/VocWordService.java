package com.vocab.estimator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vocab.estimator.entity.VocWord;
import com.vocab.estimator.dto.TestPaperDTO;
import java.util.List;

/**
 * Vocabulary word service interface
 */
public interface VocWordService extends IService<VocWord> {
    TestPaperDTO generateTestPaper(Long userId, int wordCount);
    VocWord findByWord(String word);
    List<VocWord> searchWords(String keyword);
    int getWordCount();
    List<VocWord> getRandomWordsByLevel(String level, int count);
}
