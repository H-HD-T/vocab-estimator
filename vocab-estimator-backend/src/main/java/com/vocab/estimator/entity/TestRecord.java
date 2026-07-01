package com.vocab.estimator.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("test_record")
public class TestRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String testWords;
    private Integer knownCount;
    private Integer unknownCount;
    private Integer estimateVocab;
    private Integer minRange;
    private Integer maxRange;
    private Double confidence;
    private String testType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public TestRecord() {}

    public TestRecord(Long id, Long userId, String testWords, Integer knownCount, Integer unknownCount, Integer estimateVocab, Integer minRange, Integer maxRange, Double confidence, String testType, LocalDateTime createTime) {
        this.id = id;
        this.userId = userId;
        this.testWords = testWords;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.estimateVocab = estimateVocab;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.confidence = confidence;
        this.testType = testType;
        this.createTime = createTime;
    }

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTestWords() { return this.testWords; }
    public void setTestWords(String testWords) { this.testWords = testWords; }

    public Integer getKnownCount() { return this.knownCount; }
    public void setKnownCount(Integer knownCount) { this.knownCount = knownCount; }

    public Integer getUnknownCount() { return this.unknownCount; }
    public void setUnknownCount(Integer unknownCount) { this.unknownCount = unknownCount; }

    public Integer getEstimateVocab() { return this.estimateVocab; }
    public void setEstimateVocab(Integer estimateVocab) { this.estimateVocab = estimateVocab; }

    public Integer getMinRange() { return this.minRange; }
    public void setMinRange(Integer minRange) { this.minRange = minRange; }

    public Integer getMaxRange() { return this.maxRange; }
    public void setMaxRange(Integer maxRange) { this.maxRange = maxRange; }

    public Double getConfidence() { return this.confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getTestType() { return this.testType; }
    public void setTestType(String testType) { this.testType = testType; }

    public LocalDateTime getCreateTime() { return this.createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
