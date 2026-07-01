package com.vocab.estimator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 英语词汇量估算工具 - SpringBoot 主启动类
 * 
 * 前后端分离架构，提供 RESTful API 接口
 * 核心功能：在线词汇测试、批量词表估算、语料分析、算法验证
 */
@SpringBootApplication
public class VocabEstimatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(VocabEstimatorApplication.class, args);
    }
}
