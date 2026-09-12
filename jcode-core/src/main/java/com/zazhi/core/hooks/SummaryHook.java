package com.zazhi.core.hooks;

import com.anthropic.models.messages.MessageParam;

import java.util.List;

/**
 * @author zazhi
 * @date 2026/9/6
 * @description:
 */
public class SummaryHook implements StopCallback {
    @Override
    public void onStop(List<MessageParam> messages) {
        long toolCount = messages.stream()
                // MessageParam.Content 可能是 String，也可能是 List<ContentBlockParam>
                .map(MessageParam::content)
                // 只保留 blockParams 类型的 content
                .flatMap(content -> content.blockParams().stream())
                // List<ContentBlockParam> -> ContentBlockParam
                .flatMap(List::stream)
                // 判断是不是 tool_result
                .filter(block -> block.toolResult().isPresent())
                .count();

        System.out.printf(
                "\033[90m[HOOK] Stop: session used %d tool calls\033[0m%n",
                toolCount
        );
    }
}
