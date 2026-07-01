# RAG Knowledge Base

基于 Spring AI 的 RAG 知识库问答系统，支持多格式文档上传、向量化检索和流式对话。

## 功能特性

- **多格式文档支持**：PDF、Word、Excel、Markdown 文档上传与解析
- **RAG 检索增强生成**：基于文档内容的智能问答，回答附带来源引用
- **SSE 流式对话**：实时流式返回回答内容
- **向量化存储**：本地文件向量存储，支持语义检索
- **用户认证**：JWT 认证，支持注册/登录
- **语雀同步**：支持从语雀知识库同步文档
- **Vue 前端**：配套 Web 界面

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 |
| AI 框架 | Spring AI 1.0.0 |
| 数据库 | MySQL 8.0 + Redis |
| ORM | MyBatis-Plus 3.5.5 |
| 认证 | Spring Security + JWT |
| 文档解析 | Apache PDFBox 3.0.1, Apache POI 4.1.2 |
| 前端 | Vue.js |
| Java | 17 |

## 环境要求

- JDK 17+
- MySQL 8.0+
- Redis
- Maven 3.8+

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/JAYvps/Rag-Knowledge.git
cd Rag-Knowledge
```

### 2. 创建数据库

```sql
CREATE DATABASE rag_kb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行 `src/sql/init.sql` 初始化表结构。

### 3. 配置环境变量

```bash
# AI 配置 (必填)
export AI_KEY=your-api-key
export AI_BASE_URL=https://api.example.com/v1
export AI_MODEL=qwen3.6-flash

# 数据库配置
export DB_PASSWORD=your-db-password

# JWT 密钥
export JWT_SECRET=your-jwt-secret

# 语雀配置 (可选)
export YUQUE_TOKEN=your-yuque-token
export YUQUE_NAMESPACE=your-namespace
```

或在 `application-dev.yml` 中直接配置。

### 4. 启动后端

```bash
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`

## 项目结构

```
rag-kb/
├── src/main/java/com/ragkb/
│   ├── config/          # 配置类
│   ├── controller/      # 控制器
│   ├── dto/             # 数据传输对象
│   ├── entity/          # 实体类
│   ├── mapper/          # MyBatis Mapper
│   ├── security/        # JWT 安全配置
│   ├── service/         # 业务逻辑
│   └── yuque/           # 语雀集成
├── src/main/resources/
│   ├── application.yml  # 主配置
│   └── sql/init.sql     # 数据库初始化
├── frontend/            # Vue 前端
└── data/                # 运行时数据
    ├── upload/          # 上传文件
    └── vectors/         # 向量存储
```

## API 接口

| 接口 | 说明 |
|------|------|
| `POST /auth/register` | 用户注册 |
| `POST /auth/login` | 用户登录 |
| `POST /doc/upload` | 上传文档 |
| `GET /doc/list` | 文档列表 |
| `POST /chat` | 发送对话 (SSE) |
| `POST /kb/query` | 知识库查询 |

## 配置说明

主要配置项在 `application.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: ${AI_KEY}
      base-url: ${AI_BASE_URL}
      chat:
        options:
          model: ${AI_MODEL:qwen3.6-flash}
      embedding:
        options:
          model: text-embedding-v3
```

## License

MIT
