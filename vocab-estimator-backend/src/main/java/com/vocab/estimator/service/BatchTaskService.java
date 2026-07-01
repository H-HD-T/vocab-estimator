package com.vocab.estimator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vocab.estimator.entity.BatchTask;
import com.vocab.estimator.dto.*;
import java.util.List;

/**
 * Batch task service interface
 */
public interface BatchTaskService extends IService<BatchTask> {
    BatchResultDTO processBatchWords(List<String> wordLines);
    BatchResultDTO processBatchText(String textContent);
    SamplingResultDTO runSamplingTest(int sampleLength, int knowRatio);
    List<BatchTask> getTaskHistory();
}
