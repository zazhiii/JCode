package com.zazhi.jcode;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.zazhi.jcode.hooks.*;
import com.zazhi.jcode.tools.PowerShellInput;
import com.zazhi.jcode.tools.ToolDefinitions;
import com.zazhi.jcode.tools.ToolDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zazhi
 * @date 2026/8/18
 * @description: Agent with Anthropic SDK integration
 */
public class Agent {
    List<MessageParam> history = new ArrayList<>();
    private static final int MAX_TOKENS = 8000;

    private static final Logger log = LoggerFactory.getLogger(Agent.class);

    private static final Config CONFIG = new Config();
    private static final AnthropicClient CLIENT = AnthropicOkHttpClient.builder()
            .apiKey(CONFIG.getApiKey())
            .baseUrl(stripTrailingSlash(CONFIG.getBaseUrl()))
            .build();

    private static final String SYSTEM = """
            You are a Java coding agent at %s. Use PowerShell to solve tasks. Act, don't explain."""
            .formatted(System.getProperty("user.dir"));

    private static final List<ToolUnion> TOOLS = ToolDefinitions.ALL;

    private static final Hooks HOOKS = new Hooks();

    public String query(String q) {
        history.add(
                MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .content(q)
                        .build()
        );

        HOOKS.registerUserPromptSubmitHook(new ContextInjectHook());
        HOOKS.registerPreToolUseHook(new PermissionHook());
        HOOKS.registerPreToolUseHook(new LogHook());
        HOOKS.registerPostToolUseHook(new LargeOutputHook());
        HOOKS.registerStopHook(new SummaryHook());

        HOOKS.triggerHooks(HooksEvent.USER_PROMPT_SUBMIT, q);

        log.info("User query: {}", q);
        agentLoop(history);
        MessageParam last = history.getLast();
        String resp = extractText(last);
        log.info("Agent response: {}...", resp.substring(0, Math.min(100, resp.length())));
        return resp;
    }

    private void agentLoop(List<MessageParam> messages) {
        while (true) {
            Message response = CLIENT.messages().create(
                    MessageCreateParams.builder()
                            .model(CONFIG.getModelId())
                            .system(SYSTEM)
                            .messages(messages)
                            .tools(TOOLS)
                            .maxTokens(MAX_TOKENS)
                            .build()
            );

            messages.add(
                    MessageParam.builder()
                            .role(MessageParam.Role.ASSISTANT)
                            .content(response.toParam().content())
                            .build()
            );

            // 如果不是TOOL_USE（工具调用），说明模型已完成回答
            Boolean isToolUse = response.stopReason()
                    .map(StopReason.TOOL_USE::equals)
                    .orElse(false);
            if (!isToolUse) {
                String force = HOOKS.triggerHooks(HooksEvent.STOP, history);
                // 如果有force返回，说明hook希望继续循环
                if(force != null && !force.isEmpty()) {
                    MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .content(force)
                            .build();
                    continue;
                }
                return;
            }

            // 执行每一个工具调用，收集结果
            List<ContentBlockParam> results = new ArrayList<>();
            for (ContentBlock contentBlock : response.content()) {
                if (!contentBlock.isToolUse()) continue;

                ToolUseBlock toolUseBlock = contentBlock.asToolUse();

                String blocked = HOOKS.triggerHooks(HooksEvent.PRE_TOOL_USE, toolUseBlock);
                if(blocked != null && !blocked.isEmpty()) {
                    ToolResultBlockParam toolResult =
                            ToolResultBlockParam.builder()
                                    .toolUseId(toolUseBlock.id())
                                    .content(blocked)
                                    .build();
                    results.add(ContentBlockParam.ofToolResult(toolResult));
                    continue;
                }
//                // 检查权限
//                if (!checkPermission(toolUseBlock)) {
//                    ToolResultBlockParam toolResult =
//                            ToolResultBlockParam.builder()
//                                    .toolUseId(toolUseBlock.id())
//                                    .content("Permission denied.")
//                                    .build();
//                    results.add(ContentBlockParam.ofToolResult(toolResult));
//                    continue;
//                }

                log.info("Executing tool: {} with input: {}", toolUseBlock.name(), toolUseBlock._input());
                ToolDispatcher.ToolExecution execute = ToolDispatcher.execute(toolUseBlock);

                HOOKS.triggerHooks(HooksEvent.POST_TOOL_USE, toolUseBlock, execute.output());

                ToolResultBlockParam toolResult =
                        ToolResultBlockParam.builder()
                                .toolUseId(toolUseBlock.id())
                                .content(execute.output())
                                .isError(execute.error())
                                .build();
                results.add(ContentBlockParam.ofToolResult(toolResult));
            }


            // 将工具结果添加到消息中，loop继续
            messages.add(
                    MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .contentOfBlockParams(results)
                            .build()
            );
        }
    }

    private static Boolean askUser(String name, JsonValue jsonValue, Boolean reason) {
        // TODO
        return null;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String extractText(MessageParam message) {
        MessageParam.Content content = message.content();

        // 用户直接输入的字符串消息
        if (content.isString()) {
            return content.asString();
        }

        // 模型返回的内容块消息
        if (content.isBlockParams()) {
            return content.asBlockParams().stream()
                    .filter(ContentBlockParam::isText)
                    .map(block -> block.asText().text())
                    .reduce("", String::concat);
        }

        return "";
    }
}
