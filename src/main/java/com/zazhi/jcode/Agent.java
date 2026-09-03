package com.zazhi.jcode;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.zazhi.jcode.tools.PowerShellExecutor;

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
    private static final int MAX_TOKENS = 1024;

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

    private static final List<ToolUnion> TOOLS = List.of(
            ToolUnion.ofTool(POWERSHELL_TOOL)
    );

    public String query(String q) throws Exception {
        Config config = Config.load();
        config.requireApiKey();
        AnthropicClient client = createClient(config);

        history.add(
                MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .content(q)
                        .build()
        );
        agentLoop(client, config, history);
        MessageParam last = history.getLast();
        return extractText(last);
    }

    private AnthropicClient createClient(Config config) {
        return AnthropicOkHttpClient.builder()
                .apiKey(config.getApiKey())
                .baseUrl(stripTrailingSlash(config.getBaseUrl()))
                .build();
    }

    private void agentLoop(
            AnthropicClient client,
            Config config,
            List<MessageParam> messages
    ) {
        while (true) {
            Message response = client.messages().create(
                    MessageCreateParams.builder()
                            .model(config.getModelId())
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

                        String output;
                        boolean isError = false;

                        if (!"powershell".equals(toolUseBlock.name())) {
                            output = "Error: Unknown tool: " + toolUseBlock.name();
                            isError = true;
                        } else {
                            try {
                                PowerShellInput input = toolUseBlock._input()
                                        .convert(PowerShellInput.class);

                                String command = input.command();

                                // 黄色打印命令
                                System.out.printf(
                                        "\u001B[33m$ %s\u001B[0m%n",
                                        command
                                );

                                output = PowerShellExecutor.runPowerShell(command);

                                // 只在控制台显示前 200 个字符
                                System.out.println(
                                        output.substring(
                                                0,
                                                Math.min(200, output.length())
                                        )
                                );

                                isError = output.startsWith("Error:");

                            } catch (RuntimeException e) {
                                output = "Error: Invalid tool input: " + e.getMessage();
                                isError = true;
                            }
                            ToolResultBlockParam toolResult =
                                    ToolResultBlockParam.builder()
                                            .toolUseId(toolUseBlock.id())
                                            .content(output)
                                            .isError(isError)
                                            .build();

                            results.add(
                                    ContentBlockParam.ofToolResult(toolResult)
                            );

                        }
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
        if (value == null || value.isBlank()) {
            return value;
        }
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
