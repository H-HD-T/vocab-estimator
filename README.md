# English Vocabulary Estimation Tool / 英语词汇量估算工具

基于 **SpringBoot 3.2 + Vue 3 + MySQL 8** 的前后端分离 Web 应用，支持在线词汇测试、批量词表估算、语料分析与四六级成绩相关性统计。核心词汇量估算算法自研实现，不依赖第三方 API。

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速启动](#快速启动)
- [功能模块](#功能模块)
- [API 接口一览](#api-接口一览)
- [核心算法说明](#核心算法说明)
- [算法验证](#算法验证)
- [测试数据](#测试数据)
- [许可证](#许可证)

---

## 项目概述

本项目是软件工程课程设计作品，实现了一个完整的英语词汇量估算工具，主要功能包括：

- **在线 GUI 测试**：随机抽取词汇，用户选择"认识/不认识"，自动估算词汇量
- **批处理估算**：上传词表批量计算词汇量，支持随机采样稳定性测试
- **语料分析**：上传英文文本，自动提取单词并评估文本词汇难度等级
- **算法验证**：通过 Playwright 自动访问 TestYourVocab/Preply 进行交叉验证
- **统计报表**：四六级成绩与词汇量相关性分析，可视化图表
- **词汇库管理**：标准词汇库的增删改查

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | SpringBoot | 3.2.0 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0+ |
| 前端框架 | Vue 3 | 3.5+ |
| UI 组件库 | Element Plus | 2.9+ |
| 构建工具 | Vite | 5.4+ |
| 图表 | ECharts | 5.5+ |
| HTTP 客户端 | Axios | 1.7+ |
| 自动化测试 | Playwright | (验证采集用) |
| 算法 | 自研词频加权 + 分层校准 | - |

## 项目结构

```
E:\EnglishWord\
├── backend-scrape.js              # Preply 自动化采集脚本 (Playwright)
├── sql/                           # 数据库脚本
│   ├── init.sql                   #   建表 + 基础词汇库 (144 词, K/P/F/C 四级)
│   └── add_table.sql              #   扩展表 (validation_sample)
├── docs/                          # 文档
│   ├── startup.md                 #   详细启动文档
│   └── test-cases.md              #   测试用例
├── test-data/                     # 测试数据样例
│   ├── sample-validation.json     #   算法验证数据 (testyourvocab 格式)
│   ├── sample-word-list.txt       #   批量词表示例
│   └── sample-corpus-*.txt        #   四类语料样本 (K/P/F/C)
├── vocab-estimator-backend/       # SpringBoot 后端
│   ├── pom.xml
│   └── src/main/java/com/vocab/estimator/
│       ├── VocabEstimatorApplication.java
│       ├── algorithm/             # ★ 核心估计算法
│       │   ├── VocabEstimator.java          # 算法接口
│       │   ├── AlgorithmFactory.java        # 算法工厂 (多算法组合)
│       │   ├── AlgorithmResult.java         # 结果对象
│       │   ├── AlgorithmValidator.java      # 算法验证器
│       │   ├── WordFrequencyEstimator.java  # 算法1: 词频加权估算
│       │   └── LevelCalibrationEstimator.java # 算法2: 分层难度校准
│       ├── common/                # 公共组件
│       │   ├── Result.java        #   统一返回封装
│       │   └── GlobalExceptionHandler.java  #   全局异常处理
│       ├── config/                # 配置
│       │   ├── CorsConfig.java    #   跨域配置
│       │   ├── MyBatisPlusConfig.java
│       │   └── WebConfig.java     #   静态资源配置 (图表图片)
│       ├── controller/            # REST 接口
│       │   ├── WordController.java      # 词汇库管理
│       │   ├── TestController.java      # 在线测试
│       │   ├── UserController.java      # 用户管理
│       │   ├── BatchController.java     # 批处理
│       │   ├── CorpusController.java    # 语料分析
│       │   ├── ValidationController.java # 算法验证
│       │   └── StatsController.java     # 统计报表
│       ├── dto/                   # 数据传输对象
│       ├── entity/                # 实体类
│       │   ├── VocWord.java       #   词汇库
│       │   ├── UserInfo.java      #   用户
│       │   ├── TestRecord.java    #   测试记录
│       │   ├── BatchTask.java     #   批处理任务
│       │   ├── CorpusData.java    #   语料数据
│       │   └── ValidationSample.java # 验证样本
│       ├── mapper/                # MyBatis-Plus 映射
│       └── service/               # 业务逻辑
│           └── impl/
└── vocab-estimator-frontend/      # Vue 3 前端
    ├── index.html
    ├── vite.config.js
    ├── package.json
    └── src/
        ├── main.js                # 入口
        ├── App.vue                # 根组件 (菜单导航)
        ├── router/index.js        # 路由
        ├── api/                   # API 封装
        │   ├── request.js         #   Axios 实例
        │   ├── wordApi.js
        │   ├── testApi.js
        │   ├── userApi.js
        │   ├── batchApi.js
        │   ├── corpusApi.js
        │   ├── validationApi.js
        │   └── statsApi.js
        ├── assets/
        │   └── claymorphism.css   # 校园风主题样式
        └── views/
            ├── Home.vue           # 首页
            ├── OnlineTest.vue     # 在线测试
            ├── WordLibrary.vue    # 词汇库管理
            ├── BatchManage.vue    # 批处理
            ├── CorpusAnalysis.vue # 语料分析
            ├── ValidationCompare.vue # 算法验证
            └── StatsReport.vue    # 统计报表
```

## 快速启动

### 1. 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 2. 数据库初始化

```bash
mysql -u root -p < sql/init.sql
mysql -u root -p < sql/add_table.sql
```

### 3. 修改数据库密码

编辑 `vocab-estimator-backend/src/main/resources/application.yml`，修改 `spring.datasource.password` 为你的 MySQL 密码。

### 4. 启动后端

```bash
cd vocab-estimator-backend
mvn spring-boot:run
```

后端默认运行在 **http://localhost:8088**

### 5. 启动前端

```bash
cd vocab-estimator-frontend
npm install
npm run dev
```

前端默认运行在 **http://localhost:5173**

## 功能模块

| 功能 | 前端路径 | 说明 |
|------|---------|------|
| 🏠 首页 | `/` | 功能导航、当前用户信息 |
| 📝 在线测试 | `/online-test` | 随机抽题，认识/不认识，估算词汇量+区间+确信度 |
| 📖 词汇库 | `/word-library` | 标准词汇库的增删改查，分页展示 |
| 📦 批处理 | `/batch-manage` | 上传/输入词表，批量估算，导出 Excel |
| 📊 语料分析 | `/corpus-analysis` | 上传文本，提取单词，分析词汇难度分布 |
| ✅ 算法验证 | `/validation` | TestYourVocab 交叉验证，散点图+误差分布 |
| 📈 统计报表 | `/stats` | 四六级 vs 词汇量相关性，用户历史查询 |

## API 接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| **词汇库** | | |
| GET | `/api/words/page` | 分页查询词汇 |
| POST | `/api/words` | 新增词汇 |
| PUT | `/api/words` | 更新词汇 |
| DELETE | `/api/words/{id}` | 删除词汇 |
| GET | `/api/words/count` | 词汇总数 |
| **在线测试** | | |
| GET | `/api/test/paper?userId=&count=` | 生成测试试卷 |
| POST | `/api/test/submit` | 提交测试答案 |
| GET | `/api/test/history?userId=` | 查询历史测试 |
| **批处理** | | |
| POST | `/api/batch/submit` | 批量提交词表 |
| POST | `/api/batch/sampling` | 随机采样稳定性测试 |
| POST | `/api/batch/upload` | 上传词表文件 |
| **语料分析** | | |
| POST | `/api/corpus/analyze` | 分析文本 |
| POST | `/api/corpus/upload` | 上传语料文件 |
| POST | `/api/corpus/analyze-all` | 分析全部语料 |
| **算法验证** | | |
| POST | `/api/validation/collect-one` | 自动采集一轮 (Preply) |
| POST | `/api/validation/import` | 导入验证数据 |
| POST | `/api/validation/upload` | 上传验证文件 |
| GET | `/api/validation/history` | 验证历史 |
| GET | `/api/validation/stats` | 全局统计指标 |
| POST | `/api/validation/recalculate` | 重新计算全量统计 |
| DELETE | `/api/validation/clear-data` | 清空验证数据 |
| GET | `/api/validation/sample-count` | 样本数量 |
| GET | `/charts/scatter.png` | Ci-Di 散点图 |
| GET | `/charts/histogram.png` | 误差分布直方图 |
| **统计报表** | | |
| GET | `/api/stats/correlation` | 四六级相关性 |
| GET | `/api/stats/user-history` | 用户历史分页 |
| **用户** | | |
| POST | `/api/user/login` | 登录 |
| POST | `/api/user/register` | 注册 |
| PUT | `/api/user/update` | 更新个人信息 |

## 核心算法说明

### 算法 1: 词频加权估算 (WordFrequencyEstimator)

```
输入: 一组单词 + 用户标记 (认识/不认识)
输出: {estimate, minRange, maxRange, confidence}

原理:
1.  基础估算: 已知词比例 × 参考词汇总量 (35000)
2.  词频加权: 认识低频词的权重更高（推导词汇量更大）
3.  层级调整: 高难度单词的识别率赋予更高权重
4.  确信度: 基于样本量大小和正负比例平衡度计算
```

### 算法 2: 分层难度校准 (LevelCalibrationEstimator)

```
原理:
1.  词汇分四级: K(小学) < P(初中) < F(高中) < C(大学+)
2.  累积词汇量: K=2000, P=5000, F=10000, C=20000
3.  校验规则: C/F 级用户低级别识别率需 ≥ 90%
4.  估计方式: 根据候选级别映射到累积词汇量

最终结果取两算法的平均值。
```

**参考词汇量基准**（修复后）：
| 等级 | 原值 | 修复后 | 说明 |
|------|------|--------|------|
| TOTAL_REFERENCE | 15,000 | **35,000** | 算法1 总量参考 |
| MAX_VOCAB | 20,000 | **45,000** | 算法2 上限 |
| K | 500 | **2,000** | 小学 |
| P | 2,000 | **5,000** | 初中 |
| F | 4,500 | **10,000** | 高中 |
| C | 8,000 | **20,000** | 大学+ |

## 算法验证

项目支持通过 Playwright 自动访问 TestYourVocab (Preply) 进行交叉验证：

1. 启动前端，进入"算法验证"页面
2. 点击 **"采集一轮"** 按钮
3. 自动弹出 Chrome 浏览器 → 完成词汇测试
4. 抓取数据: 认识词(Ri)、不认识词(Ui)、网站估算值(Ci)
5. 本地算法: 用 Ri/Ui 计算算法估算值(Di)
6. 对比分析: 计算 MAE、MSE、相关系数、误差分布
7. 可视化: Ci-Di 散点图 + 误差分布直方图

> 注意: Playwright 需预先安装: `npx playwright install chromium`

## 测试数据

项目自带测试数据位于 `test-data/` 目录：

| 文件 | 说明 |
|------|------|
| `sample-word-list.txt` | 批处理词表示例 (含认识/不认识标记) |
| `sample-corpus-k.txt` | K 级(小学)语料样例 |
| `sample-corpus-p.txt` | P 级(初中)语料样例 |
| `sample-corpus-f.txt` | F 级(高中)语料样例 |
| `sample-corpus-c.txt` | C 级(大学)语料样例 |
| `sample-validation.json` | 验证数据样例 (含 100 组 Ci 数据) |

## 数据库表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `voc_word` | 标准词汇库 | word, difficulty(K/P/F/C), frequency, definition, cet4/cet6 |
| `user_info` | 用户测试记录 | student_code, cet4_score, cet6_score, category |
| `test_record` | 单次测试明细 | user_id, word_list, answers, estimate, range, confidence, type |
| `batch_task` | 批处理任务 | word_text, result, status |
| `corpus_data` | 语料库 | category, raw_text, extracted_words |
| `validation_sample` | 验证样本 | known_words, unknown_words, standard_estimate, algorithm_estimate, diff |

---

## 扩展加分功能

- **服务端统一词汇库**：算法配置更新后前端自动生效
- **用户数据云端存储**：区分代号保护隐私
- **多端适配预留**：RESTful 接口支持后需小程序/桌面端对接
- **Charts 图表服务**：后端直接生成 Ci-Di 散点图和误差直方图 PNG
