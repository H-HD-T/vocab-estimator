package com.vocab.estimator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vocab.estimator.entity.TestRecord;
import com.vocab.estimator.entity.UserInfo;
import com.vocab.estimator.mapper.TestRecordMapper;
import com.vocab.estimator.mapper.UserInfoMapper;
import com.vocab.estimator.dto.StatsDTO;
import com.vocab.estimator.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class StatsServiceImpl implements StatsService {

    @Autowired private UserInfoMapper userInfoMapper;
    @Autowired private TestRecordMapper testRecordMapper;

    @Override
    public StatsDTO getCorrelationStats() {
        List<UserInfo> users = userInfoMapper.selectList(null);
        List<StatsDTO.CorrelationItem> items = new ArrayList<>();
        for (UserInfo user : users) {
            List<TestRecord> records = testRecordMapper.findByUserId(user.getId());
            if (records.isEmpty()) continue;
            double avgVocab = records.stream().mapToInt(TestRecord::getEstimateVocab).average().orElse(0);
            double avgConf = records.stream().mapToDouble(TestRecord::getConfidence).average().orElse(0);
            items.add(new StatsDTO.CorrelationItem(user.getStudentCode(), user.getCet4Score(), user.getCet6Score(), (int) avgVocab, avgConf));
        }
        return new StatsDTO(items);
    }

    @Override
    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();
        long userCount = userInfoMapper.selectCount(null);
        long testCount = testRecordMapper.selectCount(null);
        List<TestRecord> allRecords = testRecordMapper.selectList(null);
        double avgVocab = allRecords.stream().mapToInt(TestRecord::getEstimateVocab).average().orElse(0);
        double avgConf = allRecords.stream().mapToDouble(TestRecord::getConfidence).average().orElse(0);
        stats.put("userCount", userCount);
        stats.put("testCount", testCount);
        stats.put("avgVocab", Math.round(avgVocab * 100.0) / 100.0);
        stats.put("avgConfidence", Math.round(avgConf * 100.0) / 100.0);
        stats.put("cet4Correlation", 0.0);
        return stats;
    }
}
