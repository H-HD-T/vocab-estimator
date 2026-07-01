package com.vocab.estimator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocab.estimator.common.Result;
import com.vocab.estimator.entity.TestRecord;
import com.vocab.estimator.dto.*;
import com.vocab.estimator.service.TestRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private TestRecordService testRecordService;

    @GetMapping("/paper")
    public Result<TestPaperDTO> generatePaper(@RequestParam Long userId, @RequestParam(defaultValue = "40") int count) {
        return Result.success(testRecordService.generateTestPaper(userId, count));
    }

    @PostMapping("/submit")
    public Result<EstimateResultDTO> submitTest(@RequestParam Long userId, @RequestBody TestSubmitDTO submitDTO) {
        submitDTO.setUserId(userId);
        submitDTO.setTestType("GUI");
        return Result.success(testRecordService.submitTest(userId, submitDTO));
    }

    @GetMapping("/history/{userId}")
    public Result<List<TestRecord>> getHistory(@PathVariable Long userId) {
        return Result.success(testRecordService.getUserTestHistory(userId));
    }

    @GetMapping("/records")
    public Result<Page<TestRecord>> records(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(testRecordService.page(new Page<>(page, size)));
    }
}
