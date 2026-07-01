package com.vocab.estimator.service;

import com.vocab.estimator.dto.StatsDTO;
import java.util.Map;

/**
 * Statistics and report service interface
 */
public interface StatsService {
    StatsDTO getCorrelationStats();
    Map<String, Object> getOverallStats();
}
