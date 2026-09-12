package com.zazhi.core.hooks;

import com.anthropic.models.messages.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zazhi
 * @date 2026/9/5
 * @description:
 */
public class LogHook implements PreToolUseHookCallback{
    private static final Logger log = LoggerFactory.getLogger(LogHook.class);

    @Override
    public String onPreToolUse(ToolUseBlock block) {
        String input = block._input().toString();
        String argsPreview = input.substring(0, Math.min(100, input.length()));
        log.debug("{}({})", block.name(), argsPreview);
        return "";
    }
}
