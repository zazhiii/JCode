已经把 `jcode-core` / `jcode-cli` / `jcode-gui` 三个模块和核心接口都看过了。这是个很典型的"核心 UI 无关、前端各自实现"的三层结构，加 TUI 完全可以顺着现有边界走，不需要动 core。下面给一套可落地的方案。

## 一、先明确边界：TUI 是"第三个前端"

`jcode-core` 的对外契约只有三个东西：

- `AgentEngine.createSession(workspace, PermissionHandler, AgentListener)`
- `AgentSession.submit(String) -> String`（同步、阻塞、在调用线程上回调 `AgentListener`）
- `AgentEvent`（`ToolStarted/ToolFinished/PermissionResolved/TextReceived/Completed/Failed`）

GUI 就是这么接的（`JCodeApplication` 里 `this::requestPermission` + 后台 executor）。TUI 应该是**同一模式的第三个实现**：

- 不新增 core 抽象，只在需要时做**很小的**增强（见第五节）。
- 新模块里只做两件事：把 `AgentEvent` 变成界面状态，把用户按键变成 `submit` / 权限决定。

## 二、模块与依赖选择

**位置**：建议新建 `jcode-tui` 模块（保持"一个前端一个模块"的对称），再由 `jcode-cli` 依赖它、暴露成 `jcode tui` 子命令。这样打包仍是单个 shaded jar，入口统一。若只想快速验证，也可以先塞进 `jcode-cli` 的 `com.zazhi.cli.tui` 包 + 新子命令，长大后再抽模块。

**TUI 框架**，三条路：

| 方案 | 优点 | 代价 |
|---|---|---|
| **Lanterna 3.1.x** | 全屏 Screen、布局器、Panel/TextBox、双缓冲、resize 事件齐全，开发最快 | 新增依赖；它自带终端实现，和 JLine 不共用 |
| 纯 JLine（已在依赖里，3.30.0） | 零新增依赖，`Terminal` + `Display` + `AttributedString` 可控 | 布局、滚动、双缓冲全自己写，工作量大 |
| Jexer | JLine 之上带窗口/控件 | 相对小众，文档少 |

**推荐**：TUI 自己独占终端，用 **Lanterna**（`DefaultTerminalFactory().createScreen()`，private mode + 备用屏），别和 `LineReader` 混用——混用会抢光标/按键。JLine 继续只服务现有 `chat`。

## 三、关键难点：线程模型

这是最容易踩坑的地方，因为 `AgentSession.submit()` 是**同步阻塞 + 同线程回调**：

- **绝不能在 UI 线程调 `submit`**，否则权限弹窗时界面死锁。
- 用一个**单线程 `ExecutorService`** 跑 `submit`；`AgentListener` 把事件塞进 `BlockingQueue`。
- TUI 主循环 = **事件泵**：`poll` 队列 → 更新内存里的对话模型 → 重绘。Lanterna 用 `readInput()` 驱动循环（或单独线程 `take()` 后触发刷新）。
- **权限请求要跨线程握手**：`PermissionHandler.request()` 在 agent 线程被调用，此时把界面切到"确认弹窗"状态，然后**阻塞等待**（`CompletableFuture` / `CountDownLatch`），用户在 UI 线程按 y/n 后才 `complete` 放行——和 GUI 的 `answer.join()` 完全同构。

骨架大致是：

```java
// AgentListener 实现：只投递，不渲染
final class QueueListener implements AgentListener {
    private final BlockingQueue<AgentEvent> queue = new LinkedBlockingQueue<>();
    public void onEvent(AgentEvent e) { queue.offer(e); }
    public BlockingQueue<AgentEvent> queue() { return queue; }
}

// 权限：状态置为 CONFIRM 后阻塞，等 UI 线程回答
final class TuiPermissionHandler implements PermissionHandler {
    private final TuiState state; // 共享界面状态
    public PermissionDecision request(PermissionRequest req) {
        CompletableFuture<PermissionDecision> f = state.askUser(req); // 触发重绘并在UI线程等输入
        return f.join();
    }
}
```

## 四、界面与功能范围

**布局**（Lanterna `Panel` + `LinearLayout`）：

```
┌ JCode · <workspace> · <model> ─────────────────────────┐
│  对话滚动区（自动换行、可滚动）                          │
│  ● tool started  /  ✓ tool finished  /  权限: allow     │
│  assistant 回复（Markdown 渲染）                         │
├─────────────────────────────────────────────────────────┤
│ You > ____________________  (输入框)                     │
└ 状态: running/idle · 权限模式 ASK · Ctrl-C 取消 ─────────┘
```

**MVP 清单**：对话区 + 输入框 + 状态栏；实时展示工具事件；权限确认弹窗/内联；`/help /clear /new /model /workspace /exit`；滚动；窗口 resize 自适应。

**增强项**（可后置）：Esc 取消（配合 `cancel()`）、Markdown 渲染、文件编辑用 diff 高亮、工具输出可折叠、主题、鼠标滚动、历史记录。

**复用**：`doc/dev.md` 里规划的 `MarkdownTerminalRenderer` 可以抽到公共位置，让 `chat` 和 TUI 都用（TUI 里按面板宽度做换行 + `AttributedString`，正好解决中文宽度问题）。

## 五、对 core 的小增强（可选、非阻塞）

1. `AgentEvent.TextReceived` 目前只在最后整段发一次，**没有流式**。TUI 想要"打字机"效果就得让 core 支持逐块回调；不改也能用（一次性刷新）。
2. `cancel()` 只在步骤之间检查（`checkCancelled`），长工具执行中取消不灵，TUI 想做强取消需要 core 侧配合。
3. 想显示 token/耗时等，可在 `AgentEvent` 加字段——但建议**先别动**，MVP 不需要。

## 六、可测试性

把"事件 → 界面模型（行列表）"做成**纯函数/纯状态机**（不碰真实终端），渲染层只负责"行列表 → Screen"。这样绝大部分逻辑可以用 JUnit 无终端测试：喂一串 `AgentEvent`，断言产出的行内容/颜色/权限状态。core 现有测试风格（`WorkspaceToolsTest` 等）可以照搬。

## 七、落地步骤

1. 新建 `jcode-tui` 模块，加 Lanterna 依赖，`jcode-cli` 依赖它；注册 `TuiCommand`（复用 `JCodeCommand` 的 `-C / -m / --permission-mode`），并在 `JCodeCommand` 的 `subcommands` 里挂上。
2. 写 `TuiState`（对话模型 + 运行状态 + 待确认权限）与 `AgentListener`/`PermissionHandler` 两个适配器。
3. 写 `TuiApp` 主循环：`ExecutorService` 跑 `submit`、事件泵、`Screen` 重绘、输入处理、`resize`。
4. 接 MVP 命令与状态栏；补纯逻辑单测。
5. 更新 `README.md` 与 `doc/`，补一条 `jcode tui` 用法。

---

一句话总结：**TUI 当作第三个前端放进 `jcode-tui` 模块，复用 `AgentEngine`/`AgentSession`，用后台线程跑 `submit` + 事件队列 + 跨线程权限握手，选 Lanterna 图省事**。core 基本不用改，改的话优先补流式事件和真取消。

需要的话我可以直接把 `jcode-tui` 模块的骨架（pom、`TuiCommand`、`QueueListener`、`TuiPermissionHandler`、主循环空壳）搭出来跑通一个"能聊天、能确认权限"的最小版本。
