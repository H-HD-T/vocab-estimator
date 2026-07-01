# 英语词汇量估算工具 - 启动文档

## 1. 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

## 2. 数据库初始化

`ash
# 创建数据库和表结构,导入基础词汇数据
mysql -u root -p < sql/init.sql
`

会创建 ocab_estimator 数据库,包含 5 张表:
- voc_word: 标准词汇库 (144 词, K/P/F/C 四级)
- user_info: 用户信息
- test_record: 测试记录
- batch_task: 批处理任务
- corpus_data: 语料数据

## 3. 配置数据库连接

编辑 ocab-estimator-backend/src/main/resources/application.yml:

`yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vocab_estimator?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: 你的密码
`

## 4. 启动后端

`ash
cd vocab-estimator-backend
mvn spring-boot:run
`

服务默认运行在 http://localhost:8088

## 5. 启动前端

`ash
cd vocab-estimator-frontend
npm install
npm run dev
`

前端默认运行在 http://localhost:5173

## 6. 访问系统

打开浏览器访问 http://localhost:5173, 无需登录即可使用。