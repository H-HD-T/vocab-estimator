# 设计思路与代码实现逻辑

> 项目：英语词汇量估算工具
> 技术栈：SpringBoot 3.2 + Vue 3 + MySQL 8 + MyBatis-Plus

---

## 目录

1. [?目总体架构](#1-项目总体架构)
2. [核心算法模块](#2-核心算法模块)
3. [在线测试模块](#3-在线测试模块)
4. [批处理与稳定性验证模块](#4-批处理与稳定性验证模块)
5. [语料分析模块](#5-语料分析模块)
6. [算法验证模块](#6-算法验证模块)
7. [统计报表模块](#7-统计报表模块)
8. [数据库设计](#8-数据库设计)

---

## 1. 项目总体架构

### 1.1 分层架构

项目采用前后端分离架构，后端严格遵循 Controller -> Service -> Mapper -> DB 四层分层：

```
前端 (Vue 3 + Element Plus)
    | HTTP (Axios)
后端 Controller (REST API)
    | @Autowired
后端 Service (业务逻辑)
    | @Autowired
后端 Mapper (MyBatis-Plus)
    | SQL
MySQL 数据库
```

**后端包结构** (com.vocab.estimator) ：

| 包名 | 说明 | 关键文件 |
|-----------|-----------|------------------|
| `algorithm/` | 词汇量估算算法 | VocabEstimator, WordFrequencyEstimator, LevelCalibrationEstimator, AlgorithmFactory |
| `controller/` | REST 接口 | 每个功能模块对应一个 Controller |
| `service/` | 业务逻辑接口与实现 | 操作数据库、调用算法 |
| `mapper/` | MyBatis-Plus 映射器 | 数据库查询 |
| `entity/` | 实体类 | 对应数据库表 |
| `dto/` | 数据传输对象 | API 请求/响应封装 |
| `common/` | 公共组件 | 统一返回 Result、异常处理 |
| `config/` | 配置 | CORS、MyBatis-Plus、静态资源 |

### 1.2 统一返回类 \(Resul\)

所有 API 统一返回 `Result<T>` 封装，前端通过 `code` 判断是否成功（200 = 成功）：

```java
public class Result<T> {
    private int code;       // 200 成功，其他失败
    private String message; // 提示信息
    private T data;         // 数据
}
```

---

## 2. 核心算法模块

### 2.1 设计思路

算法模块是整个项目的核心，需要解决从“用户对一组单词的认识/不认识标记”推算“该用户的总词汇量”这个问题。

设计思路基于以下假设：
1. **词频权重**：认识低频词比认识高频词能推断更大的词汇量
2. **难度分层**：词汇分 K/P/F/C cz级，认识高级别词汇意味着更大的词汇量
3. **组合估计**：多种算法组合取平均，避免单一算法的偏差

### 2.2 接口设计 (VocabEstimator)

所有算法实现 `VocabEstimator` 接口：

```java
public interface VocabEstimator {
    AlgorithmResult estimate(List<Map<String, Object>> wordResults);
    String getAlgorithmName();
}
```

输入 `wordResults`：每条包含 `word`(word), `known`(true/false), `difficulty`(K/P/F/C), `frequency`(0~1)

输出 `AlgorithmResult`：

```java
public class AlgorithmResult {
    private int estimate;    // 估算词汇量
    private int minRange;    // 估算下限
    private int maxRange;    // 估算上限
    private double confidence; // 确信度 0~100%
    private int knownCount;  // 认识词数
    private int unknownCount; // 不认识词数
    private int totalWords;  // 总词数
}
```

### 2.3 算法 1: 词频加权估算 (WordFrequencyEstimator)

**设计思路：**

基于“英语常用词共计约 35,000 词”这个参考值，通过以下四步推算：

1. **基础估算**：认识词比例 x 参考词汇量
2. **词频调整**：认识低频词的权重更高，推导更大的词汇量
3. **层级调整**：高难度词汇的认识率赋予更高权重
4. **确信度**：基于样本量大小和正负比例平衡度计算

**核心代码逻辑：**

```java
// Step 1: 基础估算
int total = wordResults.size();
double knownRatio = (double) known / total;
double baseEstimate = knownRatio * TOTAL_REFERENCE_VOCAB; // 35000

// Step 2: 词频加权调整
double wordWeight = isKnown ? (1.0 - frequency + 0.1) : -(frequency + 0.1);
// 低频词被认识: 权重高
// 低频词被认识的词: weight = 0.91 ~ 1.1
// 高频词被认识的词: weight = 0.2 ~ 0.5

double freqAdjustment = (weightedScore / totalWeight) * 2000;

// Step 3: 层级调整 (K=1000, P=3000, F=6000, C=10000)
for (String level : LEVEL_BASE_VOCAB.keySet()) {
    double lr = (double) levelKnown.get(level) / levelTotal.get(level);
    int baseVocab = LEVEL_BASE_VOCAB.get(level);
    int levelMultiplier = level.equals("C") ? 4 : level.equals("F") ? 3 : ...;
    levelAdjustment += lr * baseVocab * levelMultiplier * 0.3;
}

int estimate = (int)(baseEstimate + freqAdjustment + levelAdjustment);
```

### 2.4 算法 2: 分层难度校准 (LevelCalibrationEstimator)

**设计思路：**

词汇知识是阶梯式的——C级用户应该掌握所有 K/P/F 词，但 K级用户不一定认识 C级词。通过以下步骤估计：

1. **计算各级别认识率**
2. **确定候选级别**：最高认识率≥60%的级别
3. **校验规则**：C/F级用户的低级别认识率≥90%，否则降级
4. **映射词汇量**：平均掌握度 x 最大词汇量 (45000)

### 2.5 算法工厂 (AlgorithmFactory)

```java
public AlgorithmResult estimateAll(List<Map<String, Object>> wordResults) {
    // 同时调用两个算法，取平均值
    AlgorithmResult alg1 = frequencyEstimator.estimate(wordResults);
    AlgorithmResult alg2 = calibrationEstimator.estimate(wordResults);
    
    result.setEstimate((alg1.getEstimate() + alg2.getEstimate()) / 2);
    result.setMinRange(Math.min(alg1.getMinRange(), alg2.getMinRange()));
    result.setMaxRange(Math.max(alg1.getMaxRange(), alg2.getMaxRange()));
    result.setConfidence((alg1.getConfidence() + alg2.getConfidence()) / 2);
}
```

多算法组合的好处：避免单一算法的偏差，提高估算稳定性。

---

## 3. 在线测试模块

### 3.1 设计思路

模拟真实的词汇测试流程：
1. 从标准词汇库随机抽取一定数量单词组成试卷
2. 用户对每个单词标记“认识/不认识”
3. 提交后调用算法估算词汇量
4. 保存测试记录，支持历史追踪

### 3.2 后端实现 (TestController + TestRecordService)

**生成试卷：**
```java
@GetMapping("/paper")
public Result<TestPaperDTO> generatePaper(@RequestParam int userId, @RequestParam int count) {
    // 从各难度级别分层抽样，确保试卷覆盖所有难度
    // K: count*0.25, P: count*0.25, F: count*0.25, C: count*0.25
}
```

**提交测试：**
```java
@PostMapping("/submit")
public Result<EstimateResultDTO> submitTest(@RequestBody TestSubmitDTO dto) {
    // 1. 解析用户答题标记
    // 2. 调用 AlgorithmFactory.estimateAll() 估算词汇量
    // 3. 保存测试记录到 test_record 表
    // 4. 更新 user_info 的最新词汇量
}
```

### 3.3 前端实现 (OnlineTest.vue)

核心流程：
```
加载试卷 → 逐词展示 (单词卡片) → 用户点击“认识/不认识” → 下一词
→ 全部完成 → 弹窗显示结果 (词汇量、范围、确信度)
→ 历史结果折线图 (多次测试趋势)
```

---

## 4. 批处理与稳定性验证模块

### 4.1 设计思路

批处理支持两种输入方式：
1. 文本框输入（每行一个单词 + 认识/不认识标记）
2. 上传 TXT 文件

稳定性验证设计思路：
- 从标准词汇库随机采样生成测试集
- 9 种组合（3种认识比 x 3种试卷长度）
- 每组合100次，共900次测试
- 统计均值和方差，评估算法稳定性

### 4.2 后端实现 (BatchTaskServiceImpl)

```java
// 批量计算
public BatchResultDTO processBatchText(String textContent) {
    // 解析每行文本，拆分单词和标记
    // 调用算法估算每个单词的词汇量
    // 返回统计结果
}

// 稳定性测试
public StabilityResultDTO runStabilityTest() {
    // 获取全部词汇库 (voc_word)
    // 循环 9 种组合 (10%/20%/30% x 200/300/400)
    //   每组合重复 100 次:
    //     随机有放回采样生成测试集
    //     调用算法估算
    //     记录结果
    //   计算均值和方差
    // 返回 9 个组合的统计结果
}
```

### 4.3 稳定性判定标准

```javascript
// 前端判定逻辑 (BatchManage.vue)
const cv = Math.sqrt(combo.variance) / combo.meanEstimate;
// 变异系数 CV < 15% → 稳定
// CV < 30% → 较稳定
// CV >= 30% → 不稳定
```

---

## 5. 语料分析模块

### 5.1 设计思路

语料分析的目标是根据一段英文原始文本，估算该文本的词汇难度级别，而非测试某个人的词汇量。

与在线测试的核心区别：
- 在线测试：用户标记认识/不认识 → 推算用户词汇量
- 语料分析：文本中的单词均被认为“被作者掌握” → 按难度分布估算文本难度级别

### 5.2 后端实现 (CorpusDataServiceImpl)

```java
public CorpusAnalysisDTO analyzeCorpus(Long corpusId) {
    // 1. 获取语料数据
    // 2. 提取唯一单词 (regex: [a-zA-Z]{2,})
    // 3. 对每个单词判定难度级别:
    //    - 在标准词汇库中 → 取数据库难度
    //    - 不在库中 → 按单词长度估算难度:
    //      ≤4 -> K, 5-6 -> P, 7-9 -> F, ≥10 -> C
    // 4. 加权计算估算词汇量
    //    weight: K=500, P=2000, F=5000, C=15000
    //    estimate = sum(level_ratio * level_base)
}
```

**关键区别于在线测试：** 语料分析不使用测试算法 (AlgorithmFactory)，而是直接根据难度分布计算。这种设计确保了不同难度的文本能得到明显不同的结果。

---

## 6. 算法验证模块

### 6.1 设计思路

算法验证的目标是将本系统的估算结果 (Di) 与第三方平台 testyourvocab.com (Ci) 进行对比，评估算法准确度。

验证流程：
```
自动化浏览器 (Playwright) 访问 testyourvocab.com
→ 随机勾选部分单词为“认识”
→ 网站返回估算值 Ci
→ 记录认识词 (Ri) 和不认识词 (Ui)
→ 本地算法用 Ri/Ui 估算 Di
→ 计算 Di - Ci 误差、MAE、MSE、相关系数
```

### 6.2 自动化采集 (backend-scrape.js)

使用 Playwright 模拟真实用户行为：

```javascript
// 启动浏览器 (非头部模式)
const browser = await chromium.launch({ headless: false });

// 访问 testyourvocab.com
await page.goto("https://preply.com/en/learn/english/test-your-vocab");

// 使用原生 check() 模拟勾选
await locator.check({ force: true }); // 触发 React 合成事件

// 点击 Continue
await continueBtn.click();

// 获取估算结果
const estimate = await page.evaluate(() => { /* 提取页面上的 Ci 值 */ });
```

核心技巧：使用 `locator.check()` 而非 `page.evaluate()` 内的 `element.click()`，因为前者能触发 React 的合成事件系统。

### 6.3 后端处理 (ValidationServiceImpl)

```java
public ValidationSample collectOne() {
    // 1. 启动新窗口执行 scraper
    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "node", "backend-scrape.js");
    
    // 2. 等待结果文件 (vocab_single.json)
    while (!resultFile.exists()) { Thread.sleep(3000); }
    
    // 3. 解析结果 (knownWords, unknownWords, standardEstimate=Ci)
    // 4. 调用本地算法计算 Di
    AlgorithmResult ar = algorithmFactory.estimateAll(wordResults);
    
    // 5. 保存到数据库
    // 6. 重新计算全局统计
}
```

### 6.4 误差统计指标

```java
MAE  = avg(|Di - Ci|)       // 平均绝对误差
MSE  = avg((Di - Ci)^2)     // 均方误差
RMSE = sqrt(MSE)            // 均方根误差
相关系数 = cov(Ci, Di) / (std(Ci) * std(Di)) // Pearson
误差分布: |error|<=500 / <=1000 / <=2000 / >2000
```

后端同时生成 Ci-Di 散点图和误差分布直方图为 PNG 图片（使用 Java AWT 绘制）。

---

## 7. 统计报表模块

### 7.1 设计思路

统计报表用于分析用户四六级成绩与词汇量之间的相关性，以及查看历史测试数据。

### 7.2 后端实现 (StatsServiceImpl)

```java
public StatsDTO getCorrelation() {
    // 获取所有有四六级成绩的用户
    // 计算四级分数与词汇量的相关系数 (Pearson)
    // 计算六级分数与词汇量的相关系数
    // 返回散点图数据供前端 ECharts 绘制
}
```

### 7.3 前端实现 (StatsReport.vue)

- ECharts 散点图：四级分数 vs 词汇量
- ECharts 散点图：六级分数 vs 词汇量
- 历史测试数据分页表格

---

## 8. 数据库设计

### 8.1 ER 关系

```
user_info 1---* test_record    (用户有多次测试记录)
voc_word                      (标准词汇库，独立表)
batch_task                    (批处理任务，独立表)
corpus_data                   (语料数据，独立表)
validation_sample             (验证样本，独立表)
```

### 8.2 表结构

```sql
-- voc_word: 标准词汇库 (144词)
CREATE TABLE voc_word (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(100) NOT NULL UNIQUE,     -- 单词
    difficulty VARCHAR(10),                 -- K/P/F/C 难度级别
    frequency DOUBLE DEFAULT 0.5,           -- 词频 0~1
    definition VARCHAR(500),               -- 中文释义
    cet_label VARCHAR(20) DEFAULT 'NONE',  -- CET4/CET6 标签
    create_time DATETIME
);

-- user_info: 用户测试记录
CREATE TABLE user_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_code VARCHAR(50),              -- 学号代号
    cet4_score INT,                         -- 四级分数
    cet6_score INT,                         -- 六级分数
    category VARCHAR(20)                   -- 学员类别
);

-- test_record: 单次测试明细
CREATE TABLE test_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,                         -- 用户 ID
    word_list TEXT,                         -- 测试单词列表 (JSON)
    answers TEXT,                           -- 用户答题 (JSON)
    estimate INT,                           -- 估算词汇量
    min_range INT,                          -- 估算下限
    max_range INT,                          -- 估算上限
    confidence DOUBLE,                      -- 确信度
    test_type VARCHAR(20),                  -- ONLINE/BATCH
    create_time DATETIME
);

-- batch_task: 批处理任务
CREATE TABLE batch_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word_text TEXT,                         -- 上传词表文本
    result TEXT,                            -- 批量估算结果 (JSON)
    status VARCHAR(20),                     -- 状态
    create_time DATETIME
);

-- corpus_data: 语料数据
CREATE TABLE corpus_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corpus_type VARCHAR(10),                -- 类别 C/F/P/K
    raw_text TEXT,                          -- 原始文本
    extracted_words TEXT,                   -- 提取单词 (JSON)
    analysis_result TEXT,                   -- 分析结果 (JSON)
    create_time DATETIME
);

-- validation_sample: 验证样本
CREATE TABLE validation_sample (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    known_words TEXT,                       -- 认识词 Ri (JSON)
    unknown_words TEXT,                     -- 不认识词 Ui (JSON)
    standard_estimate INT,                  -- 网站估算值 Ci
    algorithm_estimate INT,                 -- 本地算法值 Di
    diff INT,                               -- Di - Ci
    create_time DATETIME
);
```

---

## 前端路由与组件架构

```javascript
// src/router/index.js
const routes = [
  { path: '/',              component: Home },              // 首页
  { path: '/online-test',   component: OnlineTest },        // 在线测试
  { path: '/batch-manage',  component: BatchManage },       // 批处理
  { path: '/corpus-analysis', component: CorpusAnalysis },  // 语料分析
  { path: '/validation',    component: ValidationCompare }, // 算法验证
  { path: '/stats',         component: StatsReport },       // 统计报表
  { path: '/word-library',  component: WordLibrary },       // 词汇库管理
];
```

每个页面通过 api/ 目录下的封装函数调用后端接口，统一使用 Axios 实例发起 HTTP 请求。
