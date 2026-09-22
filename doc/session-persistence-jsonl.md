# JCode 本地会话持久化设计（JSONL）

状态：待实现。本文只规定实现方案，不代表代码已经具备这些能力。

## 1. 目标与范围

在 `jcode-core` 实现 CLI、GUI 共用的本地会话存储，使用户可以列出、恢复和继续指定工作区的会话。恢复后，发给模型的上下文必须包含原有的用户消息、助手消息、工具调用及工具结果，而不只是屏幕上的聊天气泡。

首版采用**每个会话一个追加式 JSONL 文件**，不引入 SQLite 或全局索引。支持异常退出后的安全恢复，但不承诺对文件修改、shell 命令等外部副作用做到“恰好执行一次”。不在首版实现全文搜索、跨设备同步、加密、会话分支、上下文压缩或自动重放未完成的工具。

现状与改造点：

- 每个会话产生一个 JSONL`~/.jcode/projects/{project-id}/{session-id}.jsonl`
- `AgentSession.history` 是内存中的 `List<MessageParam>`，其内容比 GUI 的 `ChatMessage` 丰富；后者只用于展示。
- `ChatCommand` 的 `~/.jcode/history` 是 JLine 输入历史，不是 Agent 会话。
- `AgentEngine.createSession(...)` 目前只创建临时会话；`ask` 和 GUI 也没有恢复入口。
- `AgentSession.clear()` 会直接清空上下文。持久化后不能对旧文件“清空历史”，而应创建新会话并保留旧文件。

## 2. 核心原则

1. **会话文件是事实来源。** 内存历史、CLI 输出和 GUI 气泡都由文件重建；可选的索引或缓存只能是可删除、可重建的派生数据。
2. **只追加，不原地修改。** 更名、状态变化都追加事件；删除会话是显式文件操作。
3. **模型消息与执行事件分开。** 只有 `message` 事件参与模型上下文；权限与工具执行事件用于审计和中断恢复。
4. **先持久化，再产生副作用。** 助手的工具调用消息、权限决定和 `tool_started` 必须落盘后才能执行工具。持久化失败时立即停止，不能继续执行。
5. **一个会话只允许一个写入者。** 其他进程可以只读展示，但不能同时推进同一会话。不同会话可独立写入。
6. **不静默改变模型上下文。** 恢复时遇到不支持的消息块或文件中段损坏，应报告错误，而不是略过消息继续运行。

## 3. 模块划分

新增代码位于 `jcode-core/src/main/java/com/zazhi/core/session/`：

```text
session/
├── SessionId.java                 UUID 值对象
├── SessionSummary.java            会话列表投影
├── SessionSnapshot.java           重放得到的当前状态
├── SessionEvent.java              持久化事件领域模型
├── ConversationMessage.java       模型消息领域模型
├── MessageBlock.java              text / tool_use / tool_result
├── SessionStore.java              创建、打开、列表接口
├── SessionHandle.java             单会话追加、读取、关闭接口
├── ConversationCodec.java         领域模型与 Anthropic MessageParam 转换
└── jsonl/
    ├── JsonlSessionStore.java     工作区定位、枚举和创建
    ├── JsonlSessionHandle.java    文件锁与追加写入
    ├── SessionEventCodec.java     JSON 编解码及版本检查
    └── SessionReplayer.java       顺序重放及一致性校验
```

建议的核心接口（方法名可随实现微调，但职责不应混到 UI 中）：

```java
public interface SessionStore {
    SessionHandle create(Path workspace, String modelId) throws IOException;
    SessionHandle open(Path workspace, SessionId id) throws IOException;
    List<SessionSummary> list(Path workspace) throws IOException;
}

public interface SessionHandle extends AutoCloseable {
    SessionId id();
    SessionSnapshot snapshot();
    void append(SessionEvent event) throws IOException;
    @Override void close() throws IOException;
}
```

`SessionStore` 构造时注入数据根目录（生产环境默认 `~/.jcode/sessions`，测试使用临时目录）。`SessionHandle` 在打开时取得独占写入锁，并由写入器分配 `seq`、`eventId` 和时间；调用方不能自行指定序号。如果只需要列表或预览，通过 `SessionStore` 的只读方法读取，不占用写入锁。业务代码只使用领域对象，不直接拼 JSON。

`AgentEngine` 保留现有 `createSession(...)` 作为临时会话入口，避免 `ask` 行为突然改变；新增 `createPersistedSession(...)` 和 `resumeSession(...)`。`AgentSession` 需要实现 `AutoCloseable`，在 CLI 退出、GUI 切换会话或应用关闭时释放文件锁。存储失败是硬错误，通过现有 `AgentEvent.Failed` 报告并终止当前运行，不能降级为“继续内存会话”。

会话首行的 `modelId` 仅记录初始模型；每轮 `turn_started.modelId` 记录实际使用的模型。正常续聊采用当前配置的模型（发生变化时在界面提示）；重试未完成的模型请求应使用该轮记录的模型，若当前提供方已不支持则报错并让用户显式选择新会话。API 地址、API Key 和权限模式仍由当前运行环境提供，不从会话文件恢复；恢复也不继承过去的授权决定为未来工具调用放行。

**不要直接序列化 Anthropic SDK 的 `MessageParam`。** 文件格式由 JCode 自己定义，`ConversationCodec` 负责双向转换。首版显式支持 `text`、`tool_use`、`tool_result` 三种块；未来添加图片等块时先扩展编解码与兼容性测试。工具输入保存为 JSON 值，不能把 Java 对象的 `toString()` 当作稳定格式。

JSON 库在 `jcode-core` 显式声明依赖；父 POM 已有 `fastjson2.version` 属性，但目前没有实际依赖。实现时可使用该库，采用明确的事件类型分派和字段校验，不启用任意类名反序列化。

## 4. 文件位置与命名

```text
~/.jcode/sessions/
└── <workspace-key>/
    ├── <session-uuid>.jsonl
    └── <session-uuid>.jsonl.lock
```

- 工作区必须存在。创建会话时使用 `workspace.toRealPath()` 得到规范路径；`workspace-key` 是该路径 UTF-8 字节的 SHA-256 十六进制值，不把源码目录名直接放入文件名。
- 首行 `session_created` 保存完整规范路径；打开时验证与当前工作区匹配，避免把其他项目的会话误接入当前工具权限范围。
- 会话 ID 使用 UUID。创建文件使用 `CREATE_NEW`，不能覆盖已有会话。
- 数据放在用户目录而非项目内的 `.jcode/`，避免会话中的源码、终端输出和秘密被误提交到 Git。该目录独立于已有的 JLine `history` 和 `config.properties`。
- 尽力将目录和文件权限限制为当前用户；Windows 使用平台默认 ACL，并在文档中说明 JSONL 为明文。API Key 不写入会话文件。

项目搬迁或符号链接路径变化会使工作区键变化。首版不自动合并目录；后续可提供显式导入/迁移命令，且迁移前应重新确认工作区边界。

## 5. JSONL v1 格式

UTF-8 编码，每行一个完整 JSON 对象并以 `\n` 结尾。首行固定为 `session_created`，带 `formatVersion: 1`。其余行统一包含 `seq`（从 1 严格递增）、`eventId`、`time`、`type` 和事件数据。首行的 `seq` 为 0。时间为 UTC ISO-8601 字符串；同一事件的字段名称和含义在 v1 内保持稳定。

示例只展示主要字段：

```jsonl
{"seq":0,"eventId":"...","time":"2026-09-17T08:00:00Z","type":"session_created","formatVersion":1,"sessionId":"...","workspace":"/path/to/project","modelId":"model-a"}
{"seq":1,"eventId":"...","time":"2026-09-17T08:00:01Z","type":"turn_started","turnId":"...","modelId":"model-a"}
{"seq":2,"eventId":"...","time":"2026-09-17T08:00:01Z","type":"message","messageId":"...","turnId":"...","role":"user","blocks":[{"type":"text","text":"读取 README"}]}
{"seq":3,"eventId":"...","time":"2026-09-17T08:00:02Z","type":"message","messageId":"...","turnId":"...","role":"assistant","blocks":[{"type":"tool_use","id":"toolu_1","name":"read_file","input":{"path":"README.md"}}]}
{"seq":4,"eventId":"...","time":"2026-09-17T08:00:03Z","type":"tool_started","turnId":"...","toolUseId":"toolu_1"}
{"seq":5,"eventId":"...","time":"2026-09-17T08:00:03Z","type":"tool_finished","turnId":"...","toolUseId":"toolu_1","status":"success","content":"# JCode...","isError":false}
{"seq":6,"eventId":"...","time":"2026-09-17T08:00:03Z","type":"message","messageId":"...","turnId":"...","role":"user","blocks":[{"type":"tool_result","toolUseId":"toolu_1","content":"# JCode...","isError":false}]}
{"seq":7,"eventId":"...","time":"2026-09-17T08:00:04Z","type":"message","messageId":"...","turnId":"...","role":"assistant","blocks":[{"type":"text","text":"已读取 README。"}]}
{"seq":8,"eventId":"...","time":"2026-09-17T08:00:04Z","type":"turn_finished","turnId":"...","status":"completed"}
```

事件类型及作用：

| 类型 | 必要数据 | 是否进入模型上下文 |
| --- | --- | --- |
| `session_created` | 版本、ID、工作区、初始模型 | 否 |
| `session_renamed` | 标题 | 否 |
| `turn_started` | 轮次 ID、实际使用的模型 ID | 否 |
| `message` | 轮次 ID、角色、有序块列表 | 是 |
| `permission_decided` | 工具调用 ID、allow/deny | 否 |
| `tool_started` | 工具调用 ID | 否 |
| `tool_finished` | 工具调用 ID、success/error/denied、结果内容 | 否 |
| `turn_finished` | completed/failed/cancelled、可选错误摘要 | 否 |

同一个助手消息可以包含多个 `tool_use` 块，一个后续用户消息可包含对应的多个 `tool_result` 块。模型消息的顺序以文件中 `message` 事件的顺序为准。执行事件可以辅助重建缺失的工具结果，但**不能直接当作模型消息重复送入上下文**。初始标题取首条用户文本的简短摘要；`session_renamed` 可覆盖标题。

重放时校验：`seq` 连续、轮次 ID 存在、消息角色和块组合合法、同一 `toolUseId` 的执行与结果不重复。未知事件类型或未知消息块类型不能静默跳过；当前版本无法安全恢复时以只读错误提示用户。新增可选字段可保持向后兼容；改变已有字段语义必须提升 `formatVersion` 并提供显式迁移。

## 6. 写入时序

### 普通文本轮次

```text
turn_started → 用户 message → 模型请求 → 助手 message → turn_finished(completed)
```

写入成功后才同步更新 `AgentSession` 的内存历史。模型调用失败时保留用户消息，追加 `turn_finished(failed)`；如果连失败事件也写不进去，关闭会话并向用户报告存储错误。用户消息和最终助手消息不能只存在于 UI 状态中。

### 包含工具调用的轮次

```text
模型返回完整助手消息（含全部 tool_use）
  → 写入助手 message 并落盘
  → 对每个工具：
       权限决定 → 写 permission_decided
       若拒绝：写 tool_finished(denied)，不执行
       若允许：写 tool_started 并落盘 → 执行工具 → 写 tool_finished 并落盘
  → 汇总全部工具结果，写一个含全部 tool_result 的用户 message
  → 再次调用模型
```

`tool_started` 必须在实际执行前持久化；`tool_finished` 必须在执行返回后尽快持久化。两者之间进程可能退出，此时只能认定结果**未知**。无论用 JSONL 还是 SQLite，都不能靠存储层保证外部命令恰好执行一次。

每次追加由单写入者串行完成，编码为单行字节后写入 `FileChannel`，写完换行并调用 `force(false)`；会话新建后对文件元数据执行必要的同步。尤其在执行有副作用工具之前，以及工具返回之后，必须确认相应事件写入和同步成功。同步调用可在 Agent 工作线程进行，不占用 JavaFX UI 线程。对断电场景只能提供文件系统允许范围内的尽力持久化保证。

## 7. 读取与中断恢复

启动恢复时：

1. 取得该会话的独占写入锁；拿不到锁则提示“会话正在其他进程使用”，不抢占、不混写。
2. 顺序读取到最后一个完整换行。若只有**尾部**是不完整的 JSON 行，先保留原文件备份，再移除不完整尾部；文件中段无效、序号缺口或不支持的版本直接停止恢复。
3. 重放事件，建立消息历史、标题、轮次状态和工具状态；不在重放过程中执行工具。
4. 已有最终助手消息、但缺少 `turn_finished`：补记完成事件，不再次请求模型。
5. 最后一个助手消息含 `tool_use`，但缺少完整的 `tool_result` 消息：对每个工具调用按已持久化的执行事件构造结果。已完成/已拒绝的工具复用记录的结果；只有 `tool_started` 而无结果的工具标为“执行结果未知”；尚未开始的工具标为“中断前未执行”。**均不重新执行工具**。
6. 如果所有工具已有确定结果，可直接补写完整的 `tool_result` 消息，但不自动请求模型。有未知结果时，先向用户明确提示可能已有文件或命令副作用，等待用户确认继续；确认后写入包含全部结果的 `tool_result` 消息。未知和未执行的结果使用 `isError=true` 及明确说明，避免模型误认为工具成功。此规则对 `turn_finished(failed/cancelled)` 的轮次也适用，不能因状态已结束就跳过未闭合的工具调用。
7. 若停在已保存用户消息、但还没有助手消息的模型请求处，标记为可重试；恢复会话本身不自动发起请求，由用户显式选择继续。

在旧轮次没有形成可继续的合法消息序列前，`AgentSession.submit()` 不允许直接追加新用户消息，应提示先继续待恢复轮次或创建新会话。首版不提供“在原会话里丢弃失败轮次”，避免仅清理内存而让磁盘历史与模型上下文分叉。

## 8. 并发、列表与性能

- 写入锁按**会话**而非整个工作区设置。打开固定的 `.jsonl.lock` 文件并调用 `FileChannel.tryLock()`；同一 JVM 内的 `OverlappingFileLockException` 视为已占用。锁文件可以长期存在，但是否活跃只由 OS 文件锁判断，不依赖其是否存在。不同会话互不阻塞。
- 会话列表扫描当前工作区目录的 `*.jsonl`，仅读取首行及必要的标题/更新时间事件；数量少时可直接完整扫描。按最后修改时间倒序展示，列表信息只是投影，打开时仍须完整校验。
- 大型工具输出会使单行 JSON 很大。首版要求写入的工具结果与送给模型的结果一致；若工具层引入截断，记录同样的截断内容和标记，不能让持久化历史与模型实际收到的内容不同。后续再考虑外部 blob 文件。
- 不提前做全局索引。只有当会话数量或跨项目检索的实测成本成为问题时，再从 JSONL 生成可重建的 SQLite 索引；原始会话仍可保持 JSONL。

## 9. CLI 与 GUI 接入语义

### CLI

- `jcode chat`：默认新建会话，保持当前行为；`--continue` 恢复当前工作区最近会话，`--resume <id>` 恢复指定会话。
- `/new`：关闭当前会话并新建；旧文件保留。
- `/clear`：为了保持现有“清空模型上下文”的含义，同样新建一个空会话；不能只清屏，也不能改写旧文件。两者可暂时作为别名。
- `/sessions`：列出当前工作区会话的 ID、标题、更新时间和状态；`/resume <id>` 切换会话。切换前关闭旧句柄。
- `/continue`：当恢复到未完成轮次时，经用户确认后续接该轮；正常完成的会话无需此命令，直接输入下一条即可。
- `jcode ask`：首版保持临时会话，不自动产生大量单轮文件；如有需求再加显式 `--save`。
- `--json` 仍只控制 CLI 输出格式，不与会话文件格式混淆。

### GUI

- 左侧会话区域显示当前工作区的会话列表；启动时可选最近会话，但**只加载展示，不自动运行模型或工具**。
- 选择会话时关闭旧 `AgentSession`，恢复模型历史，再从 `SessionSnapshot` 投影出 `ChatMessage`。不能把 `ObservableList<ChatMessage>` 作为恢复来源。
- “新建会话”保留旧文件；提交中禁用切换和重复发送，或先取消并等待 Agent 工作线程收束后再切换。
- JavaFX 线程只更新 UI；文件 I/O、重放和 Agent 执行放到后台线程。`stop()` 必须先处理进行中的执行，再关闭会话句柄并释放锁。

## 10. 测试与验收

核心测试全部使用临时用户数据目录和临时工作区，不依赖真实 `~/.jcode` 或真实模型网络调用；使用可控的假客户端/执行器注入故障。

| 场景 | 验收结果 |
| --- | --- |
| 普通多轮对话后重启 | 消息顺序、文本和会话 ID 一致，继续请求带完整上下文 |
| 一个助手消息调用多个工具 | 工具调用 ID 与结果一一对应，恢复后不重复执行 |
| 权限拒绝 | 不执行工具，恢复后保留错误型工具结果 |
| 工具执行后、结果写入前崩溃 | 标记结果未知，需用户确认；不自动重跑 |
| 工具结果写入后、`tool_result` 消息写入前崩溃 | 从事件重建消息，不重跑工具 |
| 最终助手消息写入后、轮次完成事件写入前崩溃 | 补记完成，不再次请求模型 |
| 最后一行被截断 | 备份后仅修复尾部，前面的有效事件可恢复 |
| 文件中段损坏或版本不支持 | 报错，不静默丢消息、不继续写入 |
| 两个进程打开同一会话 | 第二个写入者被拒绝；不同会话可并行 |
| 不同工作区与路径别名 | 会话隔离；同一真实路径得到相同工作区键 |
| 存储写入失败 | 当前运行停止，工具不在缺少前置日志时执行 |

实施顺序：先完成事件格式、文件存储、重放与故障注入测试；再改造 `AgentSession` 的写入时序和恢复 API；最后接入 CLI 命令与 GUI 会话列表。每一步都应保持 `mvn test` 通过。首版不需要把 UI、存储和 Agent 改造放在同一个大提交中。
