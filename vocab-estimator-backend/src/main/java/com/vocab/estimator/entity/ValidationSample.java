package com.vocab.estimator.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("validation_sample")
public class ValidationSample {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String knownWords;
    private String unknownWords;
    private Integer standardEstimate;
    private Integer algorithmEstimate;
    private Integer knownCount;
    private Integer unknownCount;
    private Integer diff;
    private Integer absoluteError;
    private Double relativeError;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public ValidationSample() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKnownWords() { return knownWords; }
    public void setKnownWords(String knownWords) { this.knownWords = knownWords; }
    public String getUnknownWords() { return unknownWords; }
    public void setUnknownWords(String unknownWords) { this.unknownWords = unknownWords; }
    public Integer getStandardEstimate() { return standardEstimate; }
    public void setStandardEstimate(Integer standardEstimate) { this.standardEstimate = standardEstimate; }
    public Integer getAlgorithmEstimate() { return algorithmEstimate; }
    public void setAlgorithmEstimate(Integer algorithmEstimate) { this.algorithmEstimate = algorithmEstimate; }
    public Integer getKnownCount() { return knownCount; }
    public void setKnownCount(Integer knownCount) { this.knownCount = knownCount; }
    public Integer getUnknownCount() { return unknownCount; }
    public void setUnknownCount(Integer unknownCount) { this.unknownCount = unknownCount; }
    public Integer getDiff() { return diff; }
    public void setDiff(Integer diff) { this.diff = diff; }
    public Integer getAbsoluteError() { return absoluteError; }
    public void setAbsoluteError(Integer absoluteError) { this.absoluteError = absoluteError; }
    public Double getRelativeError() { return relativeError; }
    public void setRelativeError(Double relativeError) { this.relativeError = relativeError; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
