# 一饭封神 - AI 智能菜谱生成 Agent

基于 **Spring Boot + Spring AI Alibaba Graph + Thymeleaf + MySQL + Redis Stack** 的单体应用。

## 功能概览

| 功能 | 说明 |
|------|------|
| 菜谱生成 | 根据食材 + 菜系，调用 DeepSeek 大模型生成完整菜谱 |
| 图像识别 | 上传食材照片，调用小米 MiMo 多模态模型识别 |
| Agent 工作流 | Spring AI Alibaba Graph 编排：RAG → 联网搜索 → 生成 → 营养分析 → 效果图 → 入库 |
| 美食盲盒 | 随机抽取食材 + 菜系，一键生成菜谱 |
| 满汉全席 | AI 设计整桌宴席菜单 |
| RAG | Redis Stack 向量库 + 中华菜系烹饪知识库 |
| 联网搜索 | 智谱 WebSearch |
| 营养分析 | DeepSeek 分析热量、蛋白质等 |

## 技术栈

- Java 17 / Spring Boot 3.4.5
- Spring AI 1.0.0 + Spring AI Alibaba Graph 1.0.0.2
- Thymeleaf + 原生 JS（移动端 UI）
- MyBatis-Plus + MySQL 8
- Redis Stack（向量检索 + 会话状态）

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.9+
- MySQL 8.0+（库名 `appdb`）
- **Redis Stack**（需 RediSearch 模块）

```bash
docker run -d -p 6379:6379 redis/redis-stack-server
```

### 2. 初始化数据库

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p < src/main/resources/db/schema.sql
mysql -h 127.0.0.1 -P 3306 -u root -p < src/main/resources/db/data.sql
```

### 3. 配置 API Key

任选其一：

1. 复制项目根目录 `application-local.yml.example` 为 `application-local.yml`，填写 Key 与数据库密码
2. 设置环境变量：`DEEPSEEK_API_KEY`、`DASHSCOPE_API_KEY`、`ZHIPU_API_KEY`、`MIMO_API_KEY`、`MYSQL_PASSWORD`

### 4. 启动应用

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

浏览器访问：**http://localhost:8080**

## Agent 工作流

```
START → RAG检索 → 联网搜索 → DeepSeek生成菜谱 → 营养分析 → 文生图 → 保存MySQL → END
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 主页面 |
| POST | `/api/recipe/generate` | 生成菜谱 |
| GET | `/api/recipe/{id}` | 菜谱详情 |
| GET | `/api/ingredients/categories` | 食材分类列表 |
| POST | `/api/ingredients/recognize` | 图片识别食材 |
| GET | `/api/cuisines` | 菜系列表 |
| GET | `/api/agent/session/{sessionId}` | Agent 会话状态 |
| POST | `/api/modules/blind-box/random` | 美食盲盒 |
| POST | `/api/modules/feast/generate` | 满汉全席菜单 |

## 项目结构

```
src/main/java/org/carey/masterchef/
├── config/          # AI、Graph、Redis 配置
├── controller/      # 页面 + REST API
├── domain/          # 实体与 DTO
├── graph/node/      # Graph 工作流节点
├── mapper/          # MyBatis Mapper
└── service/         # 业务与 AI 服务
```

## Python 版本

同仓库作者的 **cookieMaster** 项目为 LangChain + LangGraph Python 复刻版（默认端口 8081）。
