package com.vocab.estimator.controller;

import com.vocab.estimator.common.Result;
import com.vocab.estimator.entity.CorpusData;
import com.vocab.estimator.dto.CorpusAnalysisDTO;
import com.vocab.estimator.service.CorpusDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/corpus")
public class CorpusController {

    @Autowired
    private CorpusDataService corpusDataService;

    @PostMapping("/import")
    public Result<CorpusData> importCorpus(@RequestParam String corpusType, @RequestParam("file") MultipartFile file) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            reader.close();
            return Result.success(corpusDataService.importCorpusText(corpusType, sb.toString()));
        } catch (Exception e) {
            return Result.error("Failed to import: " + e.getMessage());
        }
    }

    @PostMapping("/import-text")
    public Result<CorpusData> importCorpusText(@RequestParam String corpusType, @RequestBody String text) {
        return Result.success(corpusDataService.importCorpusText(corpusType, text));
    }

    @GetMapping("/analyze/{corpusId}")
    public Result<CorpusAnalysisDTO> analyze(@PathVariable Long corpusId) {
        CorpusAnalysisDTO dto = corpusDataService.analyzeCorpus(corpusId);
        if (dto == null) return Result.error("Corpus not found");
        return Result.success(dto);
    }

    @GetMapping("/analyze-all")
    public Result<List<CorpusAnalysisDTO>> analyzeAll() {
        return Result.success(corpusDataService.analyzeAllCorpuses());
    }
}
