# JCode

JCode 是一个基于 Java 21 的本地 AI 编码助手。项目采用多模块结构，核心 Agent 可以被命令行和 JavaFX 图形界面复用。

## 模块

```text
JCode
├── jcode-core   Agent、会话、工具、权限和配置
├── jcode-cli    Picocli + JLine 命令行界面
└── jcode-gui    JavaFX 图形界面
```

`jcode-core` 不负责用户交互。CLI 和 GUI 分别实现权限确认及执行状态展示。

## 环境要求

- JDK 21+
- Maven 3.6+
- Windows PowerShell
- Anthropic Messages API 或兼容接口

## 配置

配置按以下优先级加载：

1. 环境变量
2. 工作区 `.jcode/config.properties`
3. 用户目录 `~/.jcode/config.properties`

环境变量：

```powershell
$env:LLM_BASE_URL = "https://api.deepseek.com/anthropic"
$env:LLM_API_KEY = "sk-xxx"
$env:LLM_MODEL_ID = "deepseek-v4-flash"
```

配置文件格式：

```properties
llm.base_url = https://api.deepseek.com/anthropic
llm.api_key = sk-xxx
llm.model_id = deepseek-v4-flash
```

真实配置不会从 `src/main/resources/config.properties` 打包进 jar。

## CLI

构建可执行 jar：

```powershell
mvn clean package
```

查看帮助和检查配置：

```powershell
java -jar jcode-cli\target\jcode-cli.jar --help
java -jar jcode-cli\target\jcode-cli.jar config doctor
```

执行一次任务：

```powershell
java -jar jcode-cli\target\jcode-cli.jar ask "分析项目并修复编译错误"
java -jar jcode-cli\target\jcode-cli.jar ask -C E:\code_java\Demo "补充单元测试"
```

启动交互式会话：

```powershell
java -jar jcode-cli\target\jcode-cli.jar chat
```

交互命令：

- `/help`：显示帮助
- `/clear`、`/new`：清空当前会话
- `/model`：显示模型
- `/workspace`：显示工作目录
- `/exit`：退出

通用参数：

- `-C, --directory`：工作目录
- `-m, --model`：临时覆盖模型
- `--permission-mode ASK|ACCEPT_EDITS|DENY`：权限策略
- `--no-color`：关闭彩色输出
- `ask --json`：最终结果使用 JSON 输出

## GUI

```powershell
mvn -pl jcode-gui -am javafx:run
```

GUI 和 CLI 使用同一个 `AgentEngine` / `AgentSession`。文件修改和潜在破坏性命令会交给前端确认。

## 安全边界

- 文件工具只能访问指定工作区。
- 危险 PowerShell 命令仍会在执行器中被阻止。
- `ASK` 模式在修改文件或运行潜在破坏性命令前询问。
- `ACCEPT_EDITS` 自动允许工作区文件编辑，但仍询问破坏性命令。
- `DENY` 拒绝所有需要授权的操作。

## 测试

```powershell
mvn test
```

当前测试覆盖配置校验、工作区读写和路径越界防护。
