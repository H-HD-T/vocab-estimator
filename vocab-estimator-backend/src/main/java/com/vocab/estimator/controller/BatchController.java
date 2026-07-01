package com.vocab.estimator.controller;

import com.vocab.estimator.common.Result;
import com.vocab.estimator.dto.*;
import com.vocab.estimator.entity.BatchTask;
import com.vocab.estimator.service.BatchTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    @Autowired
    private BatchTaskService batchTaskService;

    @PostMapping("/upload-text")
    public Result<BatchResultDTO> uploadText(@RequestBody BatchSubmitDTO dto) {
        if (dto.getTextContent() != null && !dto.getTextContent().isEmpty()) {
            return Result.success(batchTaskService.processBatchText(dto.getTextContent()));
        } else if (dto.getWordLines() != null) {
            return Result.success(batchTaskService.processBatchWords(dto.getWordLines()));
        }
        return Result.error(400, "Please provide word text content");
    }

    @PostMapping("/upload-file")
    public Result<BatchResultDTO> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return Result.success(batchTaskService.processBatchText(sb.toString()));
        } catch (Exception e) {
            return Result.error("Failed to parse file: " + e.getMessage());
        }
    }

    @PostMapping("/sampling")
    public Result<SamplingResultDTO> samplingTest(@RequestParam int sampleLength, @RequestParam int knowRatio) {
        if (knowRatio < 10 || knowRatio > 90) return Result.error(400, "knowRatio must be 10-90");
        return Result.success(batchTaskService.runSamplingTest(sampleLength, knowRatio));
    }

    @GetMapping("/history")
    public Result<List<BatchTask>> getHistory() {
        return Result.success(batchTaskService.getTaskHistory());
    }
}
