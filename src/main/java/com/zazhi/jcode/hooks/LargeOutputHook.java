package com.zazhi.jcode.hooks;

import com.anthropic.models.messages.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zazhi
 * @date 2026/9/5
 * @description:
 */
public class LargeOutputHook implements PostToolUseCallback{
    private Logger log = LoggerFactory.getLogger(LargeOutputHook.class);

    @Override
    public String onPostToolUse(ToolUseBlock block, String output) {
        if (output.length() > 100000) {
            log.info("\\033[33m[HOOK] ⚠ Large output from {}: {} chars\\033[0m", block.name(), output.length());
        }
        return null;
    }
}
