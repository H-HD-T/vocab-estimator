package com.vocab.estimator.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("corpus_data")
public class CorpusData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String corpusType;
    private String rawText;
    private String extractedWords;
    private String analysisResult;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public CorpusData() {}

    public CorpusData(Long id, String corpusType, String rawText, String extractedWords, String analysisResult, LocalDateTime createTime) {
        this.id = id;
        this.corpusType = corpusType;
        this.rawText = rawText;
        this.extractedWords = extractedWords;
        this.analysisResult = analysisResult;
        this.createTime = createTime;
    }

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public String getCorpusType() { return this.corpusType; }
    public void setCorpusType(String corpusType) { this.corpusType = corpusType; }

    public String getRawText() { return this.rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getExtractedWords() { return this.extractedWords; }
    public void setExtractedWords(String extractedWords) { this.extractedWords = extractedWords; }

    public String getAnalysisResult() { return this.analysisResult; }
    public void setAnalysisResult(String analysisResult) { this.analysisResult = analysisResult; }

    public LocalDateTime getCreateTime() { return this.createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
