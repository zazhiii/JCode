package com.zazhi.core.hooks;

import com.anthropic.models.messages.MessageParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author zazhi
 * @date 2026/9/6
 * @description:
 */
public class SummaryHook implements StopCallback {
    private static final Logger log = LoggerFactory.getLogger(SummaryHook.class);

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

        log.debug("Stop: session used {} tool calls", toolCount);
    }
}
