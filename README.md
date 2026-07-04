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
- [数据库表](#数据库表)
- [跨平台说明](#跨平台说明)

---

## 项目概述

本项目是软件工程课程设计作品，实现了一个完整的英语词汇量估算工具，主要功能包括：

- **在线 GUI 测试**：随机抽取词汇，用户选择"认识/不认识"，自动估算词汇量
- **批处理估算**：上传词表批量计算词汇量，支持随机采样稳定性测试（9种组合×100次=900轮测试）
- **语料分析**：上传英文文本，自动提取单词并评估文本词汇难度分布
- **算法验证**：通过 Playwright 自动访问 Preply (testyourvocab) 采集真实数据，交叉验证算法准确度
- **统计报表**：四六级成绩与词汇量相关性分析，可视化图表
- **词汇库管理**：标准词汇库增删改查

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
| 自动化采集 | Playwright | (算法验证用) |
| 核心算法 | 自研词频加权 + 分层校准 | - |

## 设计文档

每个功能模块的设计思路、算法原理、后端代码逻辑、前端实现方式详见 [设计文档](docs/design-document.md)。

## 项目结构

```
E:\EnglishWord\
├── backend-scrape.js              # Preply 自动化采集脚本 (Playwright)
├── package.json                   # Node 依赖 (Playwright)
├── sql/                           # 数据库脚本
│   ├── init.sql                   #   建表 (6 张表 DDL)
│   └── word_data.sql              #   ★ 7533 个单词数据导入
├── docs/                          # 文档
│   ├── design-document.md         #   设计文档
│   ├── startup.md                 #   详细启动文档
│   └── test-cases.md              #   测试用例
├── test-data/                     # 测试数据样例
│   ├── K.txt                      #   K 级(小学)语料
│   ├── P.txt                      #   P 级(初中)语料
│   ├── F.txt                      #   F 级(高中)语料
│   ├── C.txt                      #   C 级(大学)语料
│   └── sample-word-list.txt       #   批量词表示例
├── vocab-estimator-backend/       # SpringBoot 后端
│   ├── pom.xml
│   └── src/main/java/com/vocab/estimator/
│       ├── VocabEstimatorApplication.java
│       ├── algorithm/             # ★ 核心估计算法
│       │   ├── VocabEstimator.java              # 算法接口
│       │   ├── AlgorithmFactory.java            # 算法工厂
│       │   ├── AlgorithmResult.java             # 结果对象
│       │   ├── AlgorithmValidator.java          # 算法验证器
│       │   ├── WordFrequencyEstimator.java      # 算法1: 词频加权
│       │   └── LevelCalibrationEstimator.java   # 算法2: 分层校准
│       ├── common/                # 公共组件
│       │   ├── Result.java        #   统一返回封装
│       │   └── GlobalExceptionHandler.java      #   全局异常处理
│       ├── config/                # 配置
│       │   ├── CorsConfig.java    #   跨域配置
│       │   ├── MyBatisPlusConfig.java
│       │   └── WebConfig.java     #   静态资源配置
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
│       │   ├── VocWord.java
│       │   ├── UserInfo.java
│       │   ├── TestRecord.java
│       │   ├── BatchTask.java
│       │   ├── CorpusData.java
│       │   └── ValidationSample.java
│       ├── mapper/                # MyBatis-Plus Mapper
│       ├── service/               # 业务接口 + 实现
│       └── resources/
│           └── application.yml    # 数据库配置
├── vocab-estimator-frontend/      # Vue3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── App.vue
│       ├── router/index.js
│       ├── api/                   # Axios 接口封装
│       ├── views/                 # 页面组件
│       │   ├── Home.vue           #   首页
│       │   ├── OnlineTest.vue     #   在线测试
│       │   ├── BatchManage.vue    #   批处理
│       │   ├── CorpusAnalysis.vue #   语料分析
│       │   ├── AlgorithmValidation.vue # 算法验证
│       │   ├── StatsReport.vue    #   统计报表
│       │   └── WordLibrary.vue    #   词汇库管理
│       └── components/            # 公共组件
└── .gitignore
```

## 快速启动

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+
- npm 9+

### 1. 数据库初始化

```bash
# 建表
mysql -u root -p < sql/init.sql

# 导入 7533 个单词
mysql -u root -p < sql/word_data.sql
```

### 2. 后端启动

```bash
# 修改数据库密码
# 编辑 vocab-estimator-backend/src/main/resources/application.yml

cd vocab-estimator-backend
mvn spring-boot:run    # 默认端口 8088
```

### 3. 前端启动

```bash
cd vocab-estimator-frontend
npm install
npm run dev    # 默认端口 5173
```

浏览器访问 `http://localhost:5173`

### 4. Playwright 安装（算法验证用）

```bash
# 项目根目录
npm install
# 会自动安装 Chromium
```

## 功能模块

### 在线词汇测试
- 随机抽取单词，显示中文释义辅助判断
- 选择"认识/不认识"，支持 3-5 次重复测试
- 测试后展示词汇量、估算区间、确信度
- 历史测试记录折线图

### 批处理估算
- 文本框输入或上传 txt 词表
- 一键批量计算词汇量
- 稳定性验证：9 种组合 × 100 次 = 900 轮自动采样测试
- 导出 Excel 结果

### 语料分析
- 上传英文文本，自动提取不重复单词
- 分析单词在 K/P/F/C 四级中的分布
- 展示提取单词列表

### 算法验证
- 自动访问 Preply (testyourvocab) 采集真实测试数据
- 本地算法计算 Di，与网站标准值 Ci 对比
- 计算平均偏差、MAE、MSE、相关系数
- Ci-Di 散点图 + 误差分布直方图
- 每次采集结果保存入库，支持历史对比

### 统计报表
- 四六级分数与词汇量相关性分析
- 全体用户数据散点图
- 测试记录分页查询

### 词汇库管理
- 标准词汇库的增删改查
- 按难度等级 / CET 标签筛选

## API 接口一览

### 词汇库管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/word/page` | 分页查询词汇 |
| POST | `/api/word/add` | 添加单词 |
| PUT | `/api/word/update` | 更新单词 |
| DELETE | `/api/word/delete/{id}` | 删除单词 |

### 在线测试
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/test/generate?count=n` | 生成 n 道测试题 |
| POST | `/api/test/submit` | 提交测试结果 |
| GET | `/api/test/history/{userId}` | 查询用户测试历史 |

### 批处理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/batch/estimate` | 批量估算 |
| POST | `/api/batch/upload` | 上传文件估算 |
| GET | `/api/batch/tasks` | 任务列表 |
| POST | `/api/batch/stability-test` | 稳定性验证 |

### 语料分析
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/corpus/upload` | 上传语料文件 |
| POST | `/api/corpus/analyze` | 分析语料文本 |

### 算法验证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/validation/collect-one` | 采集一轮 TVY 数据 |
| GET | `/api/validation/history` | 历史采集记录 |
| GET | `/api/validation/stats` | 统计数据 |
| POST | `/api/validation/recalculate` | 重算统计指标 |
| DELETE | `/api/validation/clear-data` | 清空数据 |

### 统计报表
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/stats/correlation` | 四六级-词汇量相关性 |
| GET | `/api/stats/records` | 测试记录分页 |

## 核心算法说明

### 算法 1: 词频加权估算 (WordFrequencyEstimator)

```
输入: 一组单词 + 用户标记 (认识/不认识)
输出: {estimate, minRange, maxRange, confidence}

原理:
1. 基础估算: 已知词比例 × 参考词汇总量 (35000)
2. 词频加权: 认识低频词的权重更高（推导词汇量更大）
3. 层级调整: 高难度单词的识别率赋予更高权重
4. 确信度: 基于样本量和正负比例平衡度计算
```

### 算法 2: 分层难度校准 (LevelCalibrationEstimator)

```
原理:
1. 词汇分四级: K(小学) < P(初中) < F(高中) < C(大学+)
2. 累积词汇量: K=2000, P=5000, F=10000, C=20000
3. 校验规则: C/F 级用户低级别识别率需 ≥ 90%
4. 最终结果取两算法的加权平均
```

**参考词汇量基准**：
| 等级 | 累积词汇量 | 说明 |
|------|-----------|------|
| TOTAL_REFERENCE | 35,000 | 算法1 总量参考 |
| MAX_VOCAB | 45,000 | 算法2 上限 |
| K(小学) | 2,000 | |
| P(初中) | 5,000 | |
| F(高中) | 10,000 | |
| C(大学+) | 20,000 | |

## 算法验证

通过 Playwright 自动访问 TestYourVocab (Preply) 进行交叉验证：

1. 启动前后端，进入"算法验证"页面
2. 点击 **"采集一轮"** 按钮
3. 自动弹出 Chrome 浏览器 → 完成 2-3 页词汇勾选
4. 抓取数据: 认识词数(Ri)、不认识词数(Ui)、网站估算值(Ci)
5. 本地算法: 用 Ri/Ui 计算算法估算值(Di)
6. 对比分析: 逐行误差、平均偏差、MAE、MSE、相关系数
7. 可视化: Ci-Di 散点图 + 误差分布直方图
8. 每次结果保存至 `validation_sample` 表，支持历史数据重算

> 注意: Playwright 需预先安装，首次运行会下载 Chromium

## 测试数据

| 文件 | 说明 |
|------|------|
| `test-data/K.txt` | K 级(小学)语料样例 |
| `test-data/P.txt` | P 级(初中)语料样例 |
| `test-data/F.txt` | F 级(高中)语料样例 |
| `test-data/C.txt` | C 级(大学)语料样例 |
| `test-data/sample-word-list.txt` | 批处理词表示例 |

## 数据库表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `voc_word` | 标准词汇库 (7533词) | word, difficulty(K/P/F/C), frequency, definition, cet_label |
| `user_info` | 用户信息 | student_code, cet4_score, cet6_score, student_type |
| `test_record` | 测试记录 | user_id, test_words, known/unknown_count, estimate_vocab, min/max_range, confidence |
| `batch_task` | 批处理任务 | word_text, batch_result, status |
| `corpus_data` | 语料数据 | corpus_type, raw_text, extracted_words, analysis_result |
| `validation_sample` | 验证样本 | known_words, unknown_words, standard_estimate(Ci), algorithm_estimate(Di), diff |

## 跨平台说明

### 1. 项目路径
`backend-scrape.js` 使用 `process.cwd()` 动态获取路径，后端 `getRootDir()` 自动从 `user.dir` 向上查找 `backend-scrape.js`。

### 2. Playwright 安装
```bash
cd 项目根目录
npm install
# 自动运行 npx playwright install chromium
# 若手动安装: npx playwright install chromium
```

### 3. MySQL 配置
编辑 `vocab-estimator-backend/src/main/resources/application.yml`：
```yaml
spring.datasource.password: your_password
```

### 4. 已知限制
- 算法验证需要外网访问 `preply.com`
- 首次 Playwright 运行较慢（需下载 Chromium）
- 沙箱/无头环境下可能无法弹出 Chrome 窗口
