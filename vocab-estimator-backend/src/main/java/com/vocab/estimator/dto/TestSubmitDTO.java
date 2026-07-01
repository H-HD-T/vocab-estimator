package com.vocab.estimator.dto;

import java.util.List;
import java.util.Map;

public class TestSubmitDTO {
    private Long userId;
    private Long testId;
    private List<Map<String,Object>> answers;
    private String testType;

    public TestSubmitDTO() {}

    public TestSubmitDTO(Long userId, Long testId, List<Map<String,Object>> answers, String testType) {
        this.userId = userId;
        this.testId = testId;
        this.answers = answers;
        this.testType = testType;
    }

    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTestId() { return this.testId; }
    public void setTestId(Long testId) { this.testId = testId; }
    public List<Map<String,Object>> getAnswers() { return this.answers; }
    public void setAnswers(List<Map<String,Object>> answers) { this.answers = answers; }
    public String getTestType() { return this.testType; }
    public void setTestType(String testType) { this.testType = testType; }
}
