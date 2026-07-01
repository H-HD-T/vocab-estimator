package com.vocab.estimator.algorithm;

import com.vocab.estimator.entity.VocWord;
import com.vocab.estimator.mapper.VocWordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 算法验证模块
 * 将 testyourvocab.com 采集的验证数据集与本算法结果进行对比
 * 计算平均绝对误差、平均偏差、皮尔逊相关系数
 * 
 * 输入格式: [{knownWords:[...], unknownWords:[...], standardEstimate:N}, ...]
 * 输出: 每行 Di(算法值) vs Ci(标准值), 及总体统计指标
 */
@Component
public class AlgorithmValidator {

    @Autowired
    private VocWordMapper vocWordMapper;
    
    // 缓存已查过的单词,避免重复查询数据库
    private Map<String, VocWord> wordCache = new HashMap<>();

    /**
     * 执行验证
     * @param validationData 验证数据集 [{knownWords, unknownWords, standardEstimate}]
     * @param estimator 待验证的估算算法
     * @return 验证结果(含统计指标)
     */
    public ValidationReport validate(List<Map<String, Object>> validationData, VocabEstimator estimator) {
        List<ValidationItem> items = new ArrayList<>();
        
        // 预加载词汇库到缓存
        preloadVocabulary();
        
        for (Map<String, Object> data : validationData) {
            @SuppressWarnings("unchecked")
            List<String> knownWords = (List<String>) data.get("knownWords");
            @SuppressWarnings("unchecked")
            List<String> unknownWords = (List<String>) data.get("unknownWords");
            int standardEstimate = 0;
            Object se = data.get("standardEstimate");
            if (se instanceof Number) {
                standardEstimate = ((Number) se).intValue();
            } else if (se instanceof String) {
                standardEstimate = Integer.parseInt((String) se);
            }
            
            // 构建单词结果: 从词库中查找真实难度和词频
            List<Map<String, Object>> wordResults = new ArrayList<>();
            for (String w : knownWords) {
                wordResults.add(buildWordItem(w, true));
            }
            for (String w : unknownWords) {
                wordResults.add(buildWordItem(w, false));
            }
            
            // 调用算法估算
            AlgorithmResult result = estimator.estimate(wordResults);
            int diff = result.getEstimate() - standardEstimate;
            
            items.add(new ValidationItem(
                knownWords, unknownWords, standardEstimate, result.getEstimate(), diff
            ));
        }
        
        // 计算统计指标
        double meanError = items.stream().mapToInt(i -> Math.abs(i.getDiff())).average().orElse(0);
        double meanBias = items.stream().mapToInt(ValidationItem::getDiff).average().orElse(0);
        double correlation = computeCorrelation(items);
        
        return new ValidationReport(items, meanError, meanBias, correlation, items.size());
    }

    /**
     * 从数据库预加载所有词汇到缓存
     */
    private void preloadVocabulary() {
        if (wordCache.isEmpty()) {
            List<VocWord> allWords = vocWordMapper.selectList(null);
            for (VocWord w : allWords) {
                wordCache.put(w.getWord().toLowerCase(), w);
            }
        }
    }

    /**
     * 根据单词在词库中的记录构建算法输入项
     * 如果在词库中找不到,则使用默认值(K级, 词频0.5)
     */
    private Map<String, Object> buildWordItem(String word, boolean known) {
        Map<String, Object> item = new HashMap<>();
        item.put("word", word);
        item.put("known", known);
        
        VocWord vocWord = wordCache.get(word.toLowerCase());
        if (vocWord != null) {
            // 使用数据库中的真实难度和词频
            item.put("difficulty", vocWord.getDifficulty());
            item.put("frequency", vocWord.getFrequency() != null ? vocWord.getFrequency() : 0.5);
        } else {
            // 词库中没有该词,按长度估算难度
            item.put("difficulty", guessDifficulty(word));
            item.put("frequency", 0.5);
        }
        return item;
    }

    /**
     * 计算皮尔逊相关系数(Pearson correlation)
     * 衡量 Di 与 Ci 之间的线性相关程度
     */
    private double computeCorrelation(List<ValidationItem> items) {
        int n = items.size();
        if (n < 2) return 0;
        
        double sumStd = items.stream().mapToInt(ValidationItem::getStandardEstimate).sum();
        double sumAlg = items.stream().mapToInt(ValidationItem::getAlgorithmEstimate).sum();
        double meanStd = sumStd / n;
        double meanAlg = sumAlg / n;
        
        double cov = 0, varStd = 0, varAlg = 0;
        for (ValidationItem item : items) {
            double dStd = item.getStandardEstimate() - meanStd;
            double dAlg = item.getAlgorithmEstimate() - meanAlg;
            cov += dStd * dAlg;
            varStd += dStd * dStd;
            varAlg += dAlg * dAlg;
        }
        
        double denom = Math.sqrt(varStd * varAlg);
        return denom > 0 ? cov / denom : 0;
    }

    /**
     * 按单词长度估算难度级别(仅当词库中没有该词时使用)
     */
    private String guessDifficulty(String word) {
        int len = word.length();
        if (len <= 4) return "K";
        if (len <= 6) return "P";
        if (len <= 8) return "F";
        return "C";
    }

    // ---- 内部类: 验证项 ----
    public static class ValidationItem {
        private List<String> knownWords;
        private List<String> unknownWords;
        private int standardEstimate;
        private int algorithmEstimate;
        private int diff;
        
        public ValidationItem() {}
        public ValidationItem(List<String> kw, List<String> uw, int std, int alg, int d) {
            this.knownWords = kw; this.unknownWords = uw;
            this.standardEstimate = std; this.algorithmEstimate = alg; this.diff = d;
        }
        
        public List<String> getKnownWords() { return knownWords; }
        public List<String> getUnknownWords() { return unknownWords; }
        public int getStandardEstimate() { return standardEstimate; }
        public int getAlgorithmEstimate() { return algorithmEstimate; }
        public int getDiff() { return diff; }
    }

    /**
     * 验证报告: 含逐项明细和总体统计指标
     */
    public static class ValidationReport {
        private List<ValidationItem> items;
        private double meanError;
        private double meanBias;
        private double correlation;
        private int sampleCount;
        
        public ValidationReport() {}
        public ValidationReport(List<ValidationItem> items, double me, double mb, double corr, int sc) {
            this.items = items; this.meanError = me; this.meanBias = mb;
            this.correlation = corr; this.sampleCount = sc;
        }
        
        public List<ValidationItem> getItems() { return items; }
        public double getMeanError() { return meanError; }
        public double getMeanBias() { return meanBias; }
        public double getCorrelation() { return correlation; }
        public int getSampleCount() { return sampleCount; }
    }
}