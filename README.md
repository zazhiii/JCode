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
- Windows 10/11（PowerShell）或 macOS（系统自带 `/bin/zsh`）
- Anthropic Messages API 或兼容接口

## 配置

配置按以下优先级加载：

1. 环境变量
2. 工作区 `.jcode/config.properties`
3. 用户目录 `~/.jcode/config.properties`

Windows PowerShell 环境变量：

```powershell
$env:LLM_BASE_URL = "https://api.deepseek.com/anthropic"
$env:LLM_API_KEY = "sk-xxx"
$env:LLM_MODEL_ID = "deepseek-v4-flash"
```

macOS zsh 环境变量：

```bash
export LLM_BASE_URL="https://api.deepseek.com/anthropic"
export LLM_API_KEY="sk-xxx"
export LLM_MODEL_ID="deepseek-v4-flash"
```

配置文件格式：

```properties
llm.base_url = https://api.deepseek.com/anthropic
llm.api_key = sk-xxx
llm.model_id = deepseek-v4-flash
```

也可以通过命令行配置。`init` 会在交互终端中隐藏 API Key 输入，默认写入用户配置文件：

```shell
jcode config init
jcode config init --scope project
jcode config set llm.model_id deepseek-v4-flash
jcode config set --scope project llm.base_url https://api.deepseek.com/anthropic
printf '%s\n' "$LLM_API_KEY" | jcode config set llm.api_key --stdin
jcode config show
jcode config doctor
jcode config unset llm.model_id
```

`--scope project` 将配置写到当前工作区的 `.jcode/config.properties`；默认写到用户目录。`config show` 显示生效来源，但不会输出 API Key 明文。`ask` 和 `chat` 首次运行缺少配置时会在交互终端启动向导；非交互环境应使用环境变量或配置文件。

真实配置不会从 `src/main/resources/config.properties` 打包进 jar。

## CLI

构建可执行 JAR（Windows PowerShell 和 macOS zsh 均适用）：

```shell
mvn clean package
```

Windows PowerShell：

```powershell
java -jar jcode-cli\target\jcode-cli.jar --help
java -jar jcode-cli\target\jcode-cli.jar config doctor
java -jar jcode-cli\target\jcode-cli.jar ask "分析项目并修复编译错误"
java -jar jcode-cli\target\jcode-cli.jar ask -C E:\code_java\Demo "补充单元测试"
java -jar jcode-cli\target\jcode-cli.jar chat
```

macOS zsh：

```bash
java -jar jcode-cli/target/jcode-cli.jar --help
java -jar jcode-cli/target/jcode-cli.jar config doctor
java -jar jcode-cli/target/jcode-cli.jar ask "分析项目并修复编译错误"
java -jar jcode-cli/target/jcode-cli.jar ask -C ~/code/Demo "补充单元测试"
java -jar jcode-cli/target/jcode-cli.jar chat
```

Agent 的 `shell` 工具会根据运行平台自动选择 Windows PowerShell 或 macOS `/bin/zsh`。

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
- Windows PowerShell 和 macOS zsh 中的高风险命令会要求确认，灾难性命令会被执行器阻止。
- `ASK` 模式在修改文件或运行潜在破坏性命令前询问。
- `ACCEPT_EDITS` 自动允许工作区文件编辑，但仍询问破坏性命令。
- `DENY` 拒绝所有需要授权的操作。

## 测试

```shell
mvn test
```

当前测试覆盖配置校验、工作区读写、路径越界防护，以及跨平台 shell 选择、执行和安全规则。
