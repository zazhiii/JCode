因为上次重构时，原来由 Hook 承担的部分职责被新的“权限接口 + Agent 事件”机制替代了，但旧的 `hooks` 包没有清理，所以目前处于“新机制已经接管，旧 Hook 代码仍然保留但没有接入”的过渡状态。

原来的调用在旧 `Agent` 中：

```java
HOOKS.registerUserPromptSubmitHook(...);
HOOKS.registerPreToolUseHook(...);
HOOKS.registerPostToolUseHook(...);
HOOKS.registerStopHook(...);

HOOKS.triggerHooks(...);
```

现在旧 `Agent.java` 被拆成：

- [AgentEngine.java](E:/code_java/JCode/jcode-core/src/main/java/com/zazhi/core/AgentEngine.java)
- [AgentSession.java](E:/code_java/JCode/jcode-core/src/main/java/com/zazhi/core/AgentSession.java)

而 `AgentSession` 没有接入 `Hooks`，直接使用了新的接口。

## 原 Hook 职责去了哪里

| 原 Hook | 当前替代机制 |
|---|---|
| `PermissionHook` | `ToolPermissionPolicy + PermissionHandler` |
| `LogHook` | `AgentEvent.ToolStarted/ToolFinished` |
| `SummaryHook` | 暂时没有对应实现 |
| `ContextInjectHook` | 暂时没有对应实现 |
| `LargeOutputHook` | 工具自身的截断逻辑，但不完全等价 |

例如权限现在直接在 [AgentSession.java](E:/code_java/JCode/jcode-core/src/main/java/com/zazhi/core/AgentSession.java:119) 中处理：

```java
PermissionRequest request =
        ToolPermissionPolicy.evaluate(toolUse).orElse(null);

if (request != null) {
    PermissionDecision decision =
            permissionHandler.request(request);

    listener.onEvent(
            new AgentEvent.PermissionResolved(request, decision)
    );
}
```

工具执行状态也直接发送事件：

```java
listener.onEvent(new AgentEvent.ToolStarted(...));

ToolExecution execution = toolDispatcher.execute(toolUse);

listener.onEvent(new AgentEvent.ToolFinished(...));
```

## 为什么当时这样重构

原 Hook 存在几个问题：

1. 每次 `query()` 都重复注册 Hook，执行次数会不断增加。
2. `PermissionHook` 直接读取 `System.in`，导致 core 与 CLI 耦合。
3. `LogHook`、`SummaryHook` 直接向终端输出 ANSI 文本。
4. Hook 依赖 `Object... args` 和强制类型转换，类型安全较弱。
5. GUI、CLI 无法分别决定权限和展示形式。

所以权限和展示职责被拆成了明确的接口。

## 现在是否还需要 Hook

需要，但应该用于“可插拔的核心生命周期扩展”，而不是前端输出。

比较适合 Hook 的功能：

- 用户提示词预处理
- 上下文注入
- 敏感信息过滤
- 工具输入校验
- 工具结果压缩
- 会话统计
- 会话自动摘要
- 审计记录
- 持久化触发

例如：

```text
用户提交
  ↓ beforePrompt hooks
调用模型
  ↓
准备调用工具
  ↓ beforeTool hooks
权限检查
  ↓
执行工具
  ↓ afterTool hooks
模型完成
  ↓ afterCompletion hooks
```

## 推荐处理方式

不要继续保留当前未使用的旧 Hook，也不建议简单恢复旧代码。应该把它改造成类型安全的生命周期机制：

```java
public interface AgentHook {
    default void beforePrompt(PromptContext context) {}

    default ToolDecision beforeTool(ToolContext context) {
        return ToolDecision.continueExecution();
    }

    default void afterTool(
            ToolContext context,
            ToolExecution execution
    ) {}

    default void afterCompletion(
            CompletionContext context
    ) {}
}
```

在创建 `AgentEngine` 时注册一次：

```java
AgentEngine engine = AgentEngine.builder(config)
        .addHook(new ContextInjectionHook())
        .addHook(new LargeOutputHook())
        .addHook(new SessionPersistenceHook())
        .build();
```

然后由 `AgentSession` 在对应位置调用。

权限仍然保留独立的 `PermissionHandler`，因为权限确认需要等待 CLI、TUI 或 GUI 用户输入，不适合和普通 Hook 混在一起。

所以当前没有使用 Hook 并不是因为 Hook 完全没价值，而是上次重构只完成了权限和事件边界，旧 Hook 系统尚未重新设计并接入。当前 `hooks` 包确实属于待处理的遗留代码：下一步应该重构后重新接入，或者如果短期不需要扩展能力，就直接删除，避免产生“看起来支持 Hook、实际上不会执行”的误导。