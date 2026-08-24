# JCode

一个基于 JavaFX 的 AI 编码助手桌面应用。用户通过聊天窗口下达任务，内置 Agent 借助大模型（Claude/Anthropic 兼容接口）的 **工具调用（Tool Calling）** 能力，在工作区内自主执行 PowerShell 命令、读写文件、搜索文件，从而完成编码任务。

> 灵感类似 Claude Code / Cline 之类的 coding agent，但以本地桌面聊天软件的形式呈现。

## 功能特性

- 💬 **聊天界面**：JavaFX 实现的消息气泡界面，支持复制消息内容，用户/助手/系统/错误消息分角色展示。
- 🤖 **Agent 循环**：基于 Anthropic Java SDK 实现完整的多轮 tool-calling 循环，模型可自主决定调用工具并继续执行，直到给出最终答案。
- 🛠️ **内置工具集**：
  - `powershell`：在当前工作目录执行 PowerShell 命令
  - `read_file`：读取工作区内 UTF-8 文本文件（可限制行数）
  - `write_file`：写入文件（自动创建父目录）
  - `edit_file`：替换文件中第一处指定文本
  - `glob`：按 glob 模式在工作区查找文件
- 🛡️ **安全防护**：
  - 文件工具限制在工作区目录内，禁止越权访问（path traversal 防护）
  - PowerShell 命令屏蔽危险操作（如 `Remove-Item -Recurse`、`Format-Volume`、`shutdown` 等）
  - 命令执行超时（120s）并自动终止进程树，输出过长自动截断
- ⚙️ **灵活配置**：支持环境变量或 `config.properties` 文件配置 Base URL、API Key、模型 ID，可对接任意 Anthropic 兼容接口（默认示例为 DeepSeek）。

## 技术栈

| 组件 | 说明 |
| ---- | ---- |
| Java 21 | 语言与运行时 |
| JavaFX 21 | 桌面 UI（FXML + 自定义 ListCell） |
| anthropic-java 2.54.0 | Anthropic Messages API / 工具调用 |
| fastjson2 2.0.60 | JSON 处理 |
| SLF4J + Logback | 日志 |
| Maven | 构建管理 |

## 项目结构

```
JCode
├── pom.xml
├── src/main/java/com/zazhi/jcode/
│   ├── Main.java                      # 程序入口，启动 JavaFX
│   ├── JCodeApplication.java          # JavaFX Application，加载 FXML
│   ├── Agent.java                     # 核心：LLM agent 循环 + 工具调用
│   ├── Config.java                    # 配置加载（环境变量优先，properties 兜底）
│   ├── tools/
│   │   ├── PowerShellExecutor.java    # 安全执行 PowerShell 命令
│   │   ├── PowerShellTool.java        # 工具包装类
│   │   ├── ToolDefinitions.java       # 各工具的 JSON Schema 定义（供模型调用）
│   │   ├── ToolDispatcher.java        # 将模型发起的工具调用分发到具体实现
│   │   └── WorkspaceTools.java        # 文件读写/编辑/glob 实现（含路径安全校验）
│   └── ui/
│       ├── controller/MainController.java  # 聊天窗口控制器（异步请求）
│       ├── chat/ChatMessageCell.java       # 聊天气泡渲染
│       ├── enums/MessageRole.java          # USER/ASSISTANT/SYSTEM/ERROR
│       └── records/ChatMessage.java        # 消息数据
└── src/main/resources/
    ├── config.example.properties      # 配置示例（不含真实密钥）
    ├── config.properties              # 实际配置（已被 .gitignore 忽略）
    └── fxml/main.fxml                 # 主界面布局
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- 一个 Anthropic 兼容的 LLM API（Claude 官方或 DeepSeek 等中转服务）

### 1. 配置 API

复制示例配置并填入你的 API Key：

```powershell
Copy-Item src\main\resources\config.example.properties src\main\resources\config.properties
```

编辑 `config.properties`：

```properties
llm.base_url = https://api.deepseek.com/anthropic
llm.api_key  = sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model_id = deepseek-v4-flash
```

> `config.properties` 已在 `.gitignore` 中，不会被提交，请勿把真实密钥提交到仓库。

也可以不修改文件，改用环境变量（优先级更高）：

```powershell
$env:LLM_BASE_URL = "https://api.deepseek.com/anthropic"
$env:LLM_API_KEY  = "sk-xxx"
$env:LLM_MODEL_ID = "deepseek-v4-flash"
```

### 2. 运行

```powershell
mvn clean javafx:run
```

或先打包再运行：

```powershell
mvn clean package
java -jar target\JCode-1.0-SNAPSHOT.jar
```

### 3. 使用

1. 启动后在底部输入框输入任务，例如：*“帮我看看当前项目是干嘛的，帮我写个 readme”*。
2. Agent 会自主决定调用 `powershell` / `read_file` / `write_file` 等工具，在项目目录内完成操作。
3. 等待回复，消息气泡支持悬停显示“复制”按钮复制内容。

## 配置说明

`Config.java` 按以下优先级取值：

1. 环境变量 `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL_ID`
2. `src/main/resources/config.properties` 中的 `llm.base_url` / `llm.api_key` / `llm.model_id`

未设置 `LLM_API_KEY` 时程序会启动失败并提示。

## 安全说明

- Agent 只能操作**当前工作目录**（程序启动目录）内的文件，路径穿越会被拒绝。
- PowerShell 工具内置了危险命令黑名单与 120 秒超时，防止误操作破坏系统。
- 工具输出默认截断为 50,000 字符，避免上下文爆炸。

## 开发计划 / TODO

- [ ] 将 `PowerShellTool`（Jackson 注解包装类）纳入实际使用，或移除冗余代码
- [ ] 完善 FXML 中的“设置”菜单（当前为空）
- [ ] 支持多会话 / 会话历史持久化
- [ ] 输入中状态（发送中禁用按钮/loading 提示）目前被注释，待恢复
