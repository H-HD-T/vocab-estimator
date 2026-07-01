package com.vocab.estimator.controller;

import com.vocab.estimator.common.Result;
import com.vocab.estimator.dto.ValidationDTO;
import com.vocab.estimator.entity.ValidationSample;
import com.vocab.estimator.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/validation")
public class ValidationController {

    @Autowired
    private ValidationService validationService;

    @PostMapping("/import")
    public Result<ValidationDTO> importValidation(@RequestBody String jsonData) {
        try {
            return Result.success(validationService.importAndValidate(jsonData));
        } catch (Exception e) {
            return Result.error("Validation failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public Result<ValidationDTO> uploadValidation(@RequestParam("file") MultipartFile file) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            reader.close();
            return Result.success(validationService.importAndValidate(sb.toString()));
        } catch (Exception e) {
            return Result.error("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/collect-one")
    public Result<?> collectOne() {
        try {
            ValidationSample sample = validationService.collectOne();
            if (sample == null) {
                return Result.error("词汇库为空，无法采集");
            }
            ValidationDTO stats = validationService.recalculateAllStats();
            return Result.success(new java.util.HashMap<String, Object>() {{
                put("sample", sample);
                put("stats", stats);
            }});
        } catch (Exception e) {
            return Result.error("采集失败: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public Result<List<ValidationSample>> getHistory() {
        try {
            return Result.success(validationService.getHistory());
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result<ValidationDTO> getStats() {
        try {
            return Result.success(validationService.getStats());
        } catch (Exception e) {
            return Result.error("统计失败: " + e.getMessage());
        }
    }

    @PostMapping("/recalculate")
    public Result<ValidationDTO> recalculate() {
        try {
            return Result.success(validationService.recalculateAllStats());
        } catch (Exception e) {
            return Result.error("重算失败: " + e.getMessage());
        }
    }

    @GetMapping("/sample-count")
    public Result<?> getSampleCount() {
        try {
            int count = validationService.getHistory().size();
            return Result.success(new java.util.HashMap<String, Object>() {{
                put("sampleCount", count);
            }});
        } catch (Exception e) {
            return Result.error("读取失败");
        }
    }

    @DeleteMapping("/clear-data")
    public Result<?> clearData() {
        try {
            validationService.clearAll();
            return Result.success(new java.util.HashMap<String, Object>() {{
                put("message", "已清空");
            }});
        } catch (Exception e) {
            return Result.error("清空失败: " + e.getMessage());
        }
    }
}
