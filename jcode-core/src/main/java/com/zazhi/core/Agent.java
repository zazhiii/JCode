package com.zazhi.core;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import com.zazhi.core.permission.PermissionDecision;
import com.zazhi.core.permission.PermissionHandler;
import com.zazhi.core.permission.PermissionRequest;
import com.zazhi.core.permission.ToolPermissionPolicy;
import com.zazhi.core.tools.ShellExecutor;
import com.zazhi.core.tools.ToolDefinitions;
import com.zazhi.core.tools.ToolDispatcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

public final class Agent {
    private static final int MAX_TOKENS = 8_000;
    private static final int MAX_STEPS = 100;
    private static final List<ToolUnion> TOOLS = ToolDefinitions.ALL;

    private final PermissionHandler permissionHandler;
    private final AgentListener listener;
    private final ToolDispatcher toolDispatcher;
    private final List<MessageParam> history = new ArrayList<>();
    private final String systemPrompt;
    private volatile boolean cancelled;

    private final ConfigStore.Config config;
    private final AnthropicClient client;

    private Agent(Path workspace, PermissionHandler permissionHandler, AgentListener listener) {
        workspace = workspace.toAbsolutePath().normalize();

        this.config = new ConfigStore().load(workspace);
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(config.apiKey())
                .baseUrl(stripTrailingSlash(config.baseUrl()))
                .build();

        this.permissionHandler = permissionHandler;
        this.listener = listener;
        this.toolDispatcher = new ToolDispatcher(workspace);
        ShellExecutor.Platform platform = ShellExecutor.Platform.detect(System.getProperty("os.name"));
        this.systemPrompt = """
                You are a Java coding agent working at %s. Use the available tools to solve tasks.
                Shell commands run with %s. Generate commands that are valid for that shell.
                Act on the user's request and report the result concisely."""
                .formatted(workspace, platform.displayName());
    }

    public static Agent cteate(Path workspace, PermissionHandler permissionHandler, AgentListener listener) {
        return new Agent(workspace, permissionHandler, listener);
    }

        public synchronized String submit(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt is empty");
        }
        cancelled = false;

        // 追加用户提交的prompt
        history.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(prompt)
                .build());

        try {
            return runLoop();
        } catch (RuntimeException error) {
            listener.onEvent(new AgentEvent.Failed(error));
            throw error;
        }
    }


    public void cancel() {
        cancelled = true;
    }

    public synchronized void clear() {
        history.clear();
    }

    public synchronized int messageCount() {
        return history.size();
    }

    private String runLoop() {
        for (int step = 0; step < MAX_STEPS; step++) {
            checkCancelled();
            // 调用LLM
            Message response = client.messages().create(
                    MessageCreateParams.builder()
                            .model(config.modelId())
                            .system(systemPrompt)
                            .messages(history)
                            .tools(TOOLS)
                            .maxTokens(MAX_TOKENS)
                            .build()
            );
            // 追加LLM回应的消息
            history.add(MessageParam.builder()
                    .role(MessageParam.Role.ASSISTANT)
                    .content(response.toParam().content())
                    .build());
            // 判断停止原因是否为工具调用。是，则调用工具追加所有工具的结果；若不是，则提取最后LLM返回的消息。
            boolean usesTool = response.stopReason().map(StopReason.TOOL_USE::equals).orElse(false);
            if (!usesTool) {
                String result = extractText(history.getLast());
                listener.onEvent(new AgentEvent.TextReceived(result));
                listener.onEvent(new AgentEvent.Completed(result));
                return result;
            }

            List<ContentBlockParam> results = new ArrayList<>();
            for (ContentBlock contentBlock : response.content()) {
                checkCancelled();
                if (!contentBlock.isToolUse()) continue;
                ToolUseBlock toolUse = contentBlock.asToolUse();

                PermissionRequest request = ToolPermissionPolicy.evaluate(toolUse).orElse(null);
                if (request != null) {
                    PermissionDecision decision = permissionHandler.request(request);
                    listener.onEvent(new AgentEvent.PermissionResolved(request, decision));
                    if (decision == PermissionDecision.DENY) {
                        results.add(toolResult(toolUse.id(), "Permission denied by user", true));
                        continue;
                    }
                }

                String input = toolUse._input().toString();
                listener.onEvent(new AgentEvent.ToolStarted(toolUse.id(), toolUse.name(), input));
                ToolDispatcher.ToolExecution execution = toolDispatcher.execute(toolUse);
                listener.onEvent(new AgentEvent.ToolFinished(
                        toolUse.id(), toolUse.name(), execution.output(), execution.error()));
                results.add(toolResult(toolUse.id(), execution.output(), execution.error()));
            }

            history.add(MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .contentOfBlockParams(results)
                    .build());
        }
        throw new IllegalStateException("Agent exceeded maximum tool steps: " + MAX_STEPS);
    }

    private static ContentBlockParam toolResult(String id, String output, boolean error) {
        return ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                .toolUseId(id)
                .content(output)
                .isError(error)
                .build());
    }

    private void checkCancelled() {
        if (cancelled || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Agent execution cancelled");
        }
    }

    private static String extractText(MessageParam message) {
        MessageParam.Content content = message.content();
        if (content.isString()) return content.asString();
        if (content.isBlockParams()) {
            return content.asBlockParams().stream()
                    .filter(ContentBlockParam::isText)
                    .map(block -> block.asText().text())
                    .reduce("", String::concat);
        }
        return "";
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
