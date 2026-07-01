package com.vocab.estimator.dto;

import java.util.List;
import java.util.Map;

public class TestPaperDTO {
    private Long testId;
    private Long userId;
    private List<Map<String,Object>> words;
    private Integer totalCount;

    public TestPaperDTO() {}

    public TestPaperDTO(Long testId, Long userId, List<Map<String,Object>> words, Integer totalCount) {
        this.testId = testId;
        this.userId = userId;
        this.words = words;
        this.totalCount = totalCount;
    }

    public Long getTestId() { return this.testId; }
    public void setTestId(Long testId) { this.testId = testId; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<Map<String,Object>> getWords() { return this.words; }
    public void setWords(List<Map<String,Object>> words) { this.words = words; }
    public Integer getTotalCount() { return this.totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
}
