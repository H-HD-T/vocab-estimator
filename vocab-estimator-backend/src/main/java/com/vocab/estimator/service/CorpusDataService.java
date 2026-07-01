package com.vocab.estimator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vocab.estimator.entity.CorpusData;
import com.vocab.estimator.dto.*;
import java.util.List;

/**
 * Corpus data service interface
 */
public interface CorpusDataService extends IService<CorpusData> {
    CorpusData importCorpusText(String corpusType, String rawText);
    CorpusAnalysisDTO analyzeCorpus(Long corpusId);
    List<CorpusAnalysisDTO> analyzeAllCorpuses();
}
