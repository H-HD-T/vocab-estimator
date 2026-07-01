package com.vocab.estimator.controller;

import com.vocab.estimator.common.Result;
import com.vocab.estimator.dto.StatsDTO;
import com.vocab.estimator.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/correlation")
    public Result<StatsDTO> getCorrelation() {
        return Result.success(statsService.getCorrelationStats());
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(statsService.getOverallStats());
    }
}
