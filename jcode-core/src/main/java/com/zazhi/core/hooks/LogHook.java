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

    @Override
    public String onPreToolUse(ToolUseBlock block) {
        String argsPreview = block._input().asString().toString().substring(0, Math.min(100, block._input().asString().toString().length()));
        System.out.printf("\033[90m[HOOK] %s(%s)\033[0m%n", block.name(), argsPreview);
        return "";
    }
}
