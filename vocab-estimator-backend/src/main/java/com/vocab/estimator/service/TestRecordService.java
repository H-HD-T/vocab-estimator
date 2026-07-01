package com.vocab.estimator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vocab.estimator.entity.TestRecord;
import com.vocab.estimator.dto.*;
import java.util.List;

/**
 * Test record service interface
 */
public interface TestRecordService extends IService<TestRecord> {
    TestPaperDTO generateTestPaper(Long userId, int wordCount);
    EstimateResultDTO submitTest(Long userId, TestSubmitDTO submitDTO);
    List<TestRecord> getUserTestHistory(Long userId);
}
