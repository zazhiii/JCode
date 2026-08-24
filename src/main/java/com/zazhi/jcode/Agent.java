package com.zazhi.jcode;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonField;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.zazhi.jcode.tools.PowerShellExecutor;
import com.zazhi.jcode.tools.ToolDefinitions;
import com.zazhi.jcode.tools.ToolDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zazhi
 * @date 2026/8/18
 * @description: Agent with Anthropic SDK integration
 */
public class Agent {
    List<MessageParam> history = new ArrayList<>();
    private static final int MAX_TOKENS = 8000;

//    private static final Path WORKING_DIRECTORY = Path.of("")
//            .toAbsolutePath()
//            .normalize();

        private static final Logger log =
            LoggerFactory.getLogger(Agent.class);

    private static final Config CONFIG = new Config();
    private static final AnthropicClient CLIENT = AnthropicOkHttpClient.builder()
            .apiKey(CONFIG.getApiKey())
            .baseUrl(stripTrailingSlash(CONFIG.getBaseUrl()))
            .build();

    private static final String SHELL_NAME = "Windows cmd";

    private static final String SYSTEM = """
            You are a Java coding agent at %s. Use PowerShell to solve tasks. Act, don't explain."""
            .formatted(System.getProperty("user.dir"));

    private static final Tool POWERSHELL_TOOL = Tool.builder()
            .name("powershell")
            .description("Run a PowerShell command.")
            .inputSchema(
                    Tool.InputSchema.builder()
                            .type(JsonValue.from("object"))
                            .properties(
                                    Tool.InputSchema.Properties.builder()
                                            .putAdditionalProperty(
                                                    "command",
                                                    JsonValue.from(
                                                            Map.of("type", "string")
                                                    )
                                            )
                                            .build()
                            )
                            .required(List.of("command"))
                            .build()
            )
            .build();

    private static final List<ToolUnion> TOOLS = ToolDefinitions.ALL;

    public String query(String q) {
        history.add(
                MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .content(q)
                        .build()
        );
        log.info("User query: {}", q);
        agentLoop(history);
        MessageParam last = history.getLast();
        // 从MessageParam中提取文本内容
        String resp = extractText(last);
        log.info("Agent response: {}", resp);
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


            Boolean isToolUse = response.stopReason()
                    .map(StopReason.TOOL_USE::equals)
                    .orElse(false);
            // 如果不是TOOL_USE（工具调用），说明模型已完成回答
            if (!isToolUse) {
                return;
            }

            // 执行每一个工具调用，收集结果
            List<ContentBlockParam> results = new ArrayList<>();
            response.content().stream()
                    .filter(ContentBlock::isToolUse)
                    .forEach(contentBlock -> {
                        ToolUseBlock toolUseBlock = contentBlock.asToolUse();

                        log.info("Executing tool: {} with input: {}", toolUseBlock.name(), toolUseBlock._input());
                        ToolDispatcher.ToolExecution execute = ToolDispatcher.execute(toolUseBlock);

                        String output = execute.output();
                        boolean isError = execute.error();

                        ToolResultBlockParam toolResult =
                                ToolResultBlockParam.builder()
                                        .toolUseId(toolUseBlock.id())
                                        .content(output)
                                        .isError(isError)
                                        .build();

                        results.add(ContentBlockParam.ofToolResult(toolResult));
                    });

            // 将工具结果添加到消息中，loop继续
            messages.add(
                    MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .contentOfBlockParams(results)
                            .build()
            );
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record PowerShellInput(String command) {
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
