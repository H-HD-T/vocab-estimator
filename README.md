# English Vocabulary Estimation Tool / 英语词汇量估算工具

基于 **SpringBoot 3.2 + Vue 3 + MySQL 8** 的前后端分离 Web 应用，支持在线词汇测试、批量词表估算、语料分析与四六级成绩相关性统计。核心词汇量估算算法自研实现，不依赖第三方 API。

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端 | Java SpringBoot | 3.2.0 |
| 数据库 | MySQL | 8.0+ |
| ORM | MyBatis-Plus | 3.5.5 |
| 构建 | Maven | 3.8+ |
| 前端 | Vue 3 | 3.4+ |
| UI 框架 | Element Plus | 2.5+ |
| 构建工具 | Vite | 5.x |
| 爬虫验证 | Playwright (Node.js) | 最新 |

## 项目结构

`
E:\EnglishWord\
├── vocab-estimator-backend/       # SpringBoot 后端
│   ├── src/main/java/com/vocab/estimator/
│   │   ├── algorithm/             # 词汇量估算算法（核心）
│   │   ├── config/                # 配置（跨域、MyBatis-Plus）
│   │   ├── controller/            # REST 控制器
│   │   ├── dto/                   # 数据传输对象
│   │   ├── entity/                # 实体类
│   │   ├── mapper/                # MyBatis-Plus Mapper
│   │   └── service/               # 业务逻辑层
│   └── src/main/resources/
│       └── application.yml        # 配置文件
├── vocab-estimator-frontend/      # Vue 3 前端
│   └── src/
│       ├── api/                   # API 封装
│       ├── views/                 # 页面组件
│       ├── router/                # 路由
│       └── App.vue                # 根组件
├── sql/                           # 数据库脚本
│   ├── init.sql                   # 建表语句
│   └── word_data.sql              # 词汇数据
├── docs/                          # 文档
│   ├── startup.md                 # 启动文档
│   ├── test-cases.md              # 测试用例
│   └── design-document.md         # 设计文档
├── test-data/                     # 测试数据
│   ├── batch_test.txt             # 批处理测试样本
│   └── sample_corpus.txt          # 语料测试样本
├── backend-scrape.js              # Playwright 爬虫（算法验证）
└── README.md                      # 本文件
`

## 快速启动

### 前置要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+
- Chrome/Edge 浏览器（算法验证爬虫需要）

### 1. 初始化数据库

`ash
mysql -u root -p < sql/init.sql
mysql -u root -p < sql/word_data.sql
`

### 2. 启动后端

`ash
cd vocab-estimator-backend
mvn spring-boot:run
`

后端运行在 http://localhost:8088

### 3. 启动前端

`ash
cd vocab-estimator-frontend
npm install
npm run dev
`

前端运行在 http://localhost:5173

### 4. 配置数据库连接

编辑 ocab-estimator-backend/src/main/resources/application.yml：

`yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vocab_estimator?useUnicode=true&characterEncoding=utf-8
    username: root
    password: 你的密码
`

## 功能模块

### 1. 在线词汇测试
随机抽取词汇生成试卷，用户标记认识/不认识，提交后算法估算词汇量并保存测试记录。

### 2. 批处理任务
上传词表文本（或文本框输入），批量调用估算算法，返回每条词表对应的词汇量结果。

### 3. 语料分析
上传文本语料（C/F/P/K 类学员），自动提取全部单词，分析词汇水平分布。

### 4. 算法验证
Playwright 爬虫自动访问 testyourvocab.com（Preply），获取网站估算结果 Ci，与本系统算法输出 Di 进行对比验证，计算误差、方差、相关系数，生成可视化图表。

### 5. 统计报表
展示四六级分数与词汇量相关性散点图、历史测试数据分页查询。

### 6. 词汇库管理
标准词汇库的增删改查，覆盖 K/P/F/C 四个难度等级。

## 核心算法说明

采用幂律公式估算词汇量：

`
Di = 40000 × (known / total)^1.5
`

其中 known 为用户认识的单词数，	otal 为测试总词数，^1.5 幂次压缩了比例（认识50%的词对应约28%的词汇量），更符合语言习得规律。

输入：一组单词 + 用户标记（认识/不认识）+ 难度等级（K/P/F/C）
输出：{estimate: 词汇量数值, minRange: 下限, maxRange: 上限, confidence: 确信度百分比}

## 算法验证

验证流程：
1. Playwright 爬虫访问 Preply 词汇测试页，根据概率模型（mastery^等级）决定认识/不认识
2. 爬虫提取网站词汇量估算值 Ci
3. 同一组词列表输入本系统算法得出 Di
4. 累加历史数据，计算 MAE、MSE、Pearson 相关系数
5. 生成散点图和误差分布直方图

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/login | 用户登录/注册 |
| GET | /api/test/generate?count=N | 生成测试试卷 |
| POST | /api/test/submit | 提交测试结果 |
| GET | /api/test/history?userId=X | 查询用户测试历史 |
| POST | /api/batch/estimate | 批量词表估算 |
| POST | /api/batch/sampling | 采样稳定性测试 |
| POST | /api/corpus/analyze | 语料分析 |
| POST | /api/validation/collect-one | 单次算法验证采集 |
| GET | /api/validation/history | 验证历史 |
| POST | /api/validation/clear-all | 清空验证数据 |
| GET | /api/word/page | 词汇库分页查询 |
| POST | /api/word | 添加单词 |
| PUT | /api/word | 修改单词 |
| DELETE | /api/word/{id} | 删除单词 |
| GET | /api/stats/correlation | 四六级相关性统计 |

## 数据库表

| 表名 | 说明 |
|------|------|
| voc_word | 标准词汇库（单词、难度等级、词频、释义、四六级标签） |
| user_info | 用户测试记录 |
| test_record | 单次测试明细 |
| batch_task | 批处理任务表 |
| corpus_data | 语料库数据 |
| validation_sample | 算法验证样本表 |

## License

MIT
