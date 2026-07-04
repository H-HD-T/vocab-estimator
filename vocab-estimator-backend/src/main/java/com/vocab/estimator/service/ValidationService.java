package com.vocab.estimator.service;

import com.vocab.estimator.dto.ValidationDTO;
import com.vocab.estimator.entity.ValidationSample;
import java.util.List;
import java.util.Map;

public interface ValidationService {
    ValidationDTO validateAlgorithm(List<Map<String, Object>> validationData);
    ValidationDTO importAndValidate(String jsonData);
    ValidationSample collectOne();
    List<ValidationSample> getHistory();
    ValidationDTO getStats();
    ValidationDTO recalculateAllStats();
    Map<String, String> lookupDifficulties(List<String> words);
    void clearAll();
}
