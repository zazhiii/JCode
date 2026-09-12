package com.zazhi.core.hooks;

import com.anthropic.models.messages.ToolUseBlock;

/**
 * @author zazhi
 * @date 2026/9/5
 * @description:
 */
public interface PreToolUseHookCallback extends HooksCallback{
    String onPreToolUse(ToolUseBlock block);
}
