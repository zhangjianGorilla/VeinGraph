# 脉络 (VeinGraph) - 基于 LLM 与图数据库的动态关系抽取与对话系统

## 项目简介

VeinGraph 是一个基于大语言模型(LLM)与图数据库的人物关系分析对话系统，能够从文本中自动抽取人物关系并支持自然语言问答。

## 技术栈

- **后端**: Spring Boot 3.4 + Maven (Java 17)
- **图数据库**: Neo4j
- **LLM**: Ollama (Qwen2.5-7B)
- **前端**: React 18 + Vite + AntV G6 + Ant Design

## 快速开始

### 环境要求

- Java 17+
- Maven 3.8+
- Neo4j 5.x
- Ollama (已下载 qwen2.5:7b 模型)

### 配置

1. 启动 Neo4j 数据库
2. 启动 Ollama 服务 (`ollama serve`)
3. 修改 `application.yml` 中的数据库和LLM配置

### 运行

```bash
mvn spring-boot:run
```

## 许可证

[MIT License](LICENSE)