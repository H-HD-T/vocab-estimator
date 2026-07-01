# English Vocabulary Estimation Tool / 英语词汇量估算工具

基于 SpringBoot 3.2 + Vue 3 + MySQL 8 的前后端分离 Web 应用,支持在线词汇测试、批量词表估算、语料分析与四六级成绩相关性统计。

## 项目结构

`
E:\EnglishWord\
├── vocab-estimator-backend/    # SpringBoot 后端 (Java 17)
│   ├── src/main/java/com/vocab/estimator/
│   │   ├── algorithm/          # 核心估计算法
│   │   ├── common/             # 公共响应封装、异常处理
│   │   ├── config/             # 跨域、MyBatis-Plus 配置
│   │   ├── controller/         # REST 接口
│   │   ├── dto/                # 数据传输对象
│   │   ├── entity/             # 实体类
│   │   ├── mapper/             # MyBatis-Plus Mapper
│   │   └── service/            # 业务逻辑层
│   └── src/main/resources/
│       └── application.yml     # 数据库等配置
├── vocab-estimator-frontend/   # Vue 3 前端
│   └── src/
│       ├── api/                # API 封装 (Axios)
│       ├── router/             # 路由配置
│       └── views/              # 页面组件
├── sql/
│   └── init.sql                # 建表 SQL + 基础词汇数据
├── docs/
│   ├── startup.md              # 启动文档
│   └── test-cases.md           # 测试用例
├── test-data/                  # 测试数据样例
└── README.md
`

## 快速启动

### 1. 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 2. 数据库初始化

`ash
mysql -u root -p < sql/init.sql
`

### 3. 修改数据库密码

编辑 ocab-estimator-backend/src/main/resources/application.yml, 修改 spring.datasource.password 为你的 MySQL 密码。

### 4. 启动后端

`ash
cd vocab-estimator-backend
mvn spring-boot:run
`

后端默认运行在 http://localhost:8088

### 5. 启动前端

`ash
cd vocab-estimator-frontend
npm install
npm run dev
`

前端默认运行在 http://localhost:5173

## 功能模块

| 功能 | 路径 | 说明 |
|---|---|---|
| 在线测试 | /online-test | 随机抽题,选择题形式估算词汇量 |
| 批处理 | /batch-manage | 上传词表批量估算,采样稳定性测试 |
| 语料分析 | /corpus-analysis | 输入英文文本分析词汇水平 |
| 算法验证 | /validation | 与 TestYourVocab 数据对比验证 |
| 统计报表 | /stats | 四六级成绩与词汇量相关性 |
| 词汇库 | /word-library | 标准词汇库管理 |

## 技术栈

- **后端**: SpringBoot 3.2, MyBatis-Plus 3.5.5, MySQL 8
- **前端**: Vue 3, Element Plus, Vite 5, ECharts, Axios
- **算法**: 词频加权估算 + 分层难度校准 (自研,不依赖第三方 API)