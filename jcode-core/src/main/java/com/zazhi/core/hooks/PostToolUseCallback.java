package com.zazhi.core.hooks;

import com.anthropic.models.messages.ToolUseBlock;

/**
 * @author zazhi
 * @date 2026/9/5
 * @description:
 */
@FunctionalInterface
public interface PostToolUseCallback extends HooksCallback{
    String onPostToolUse(ToolUseBlock block, String output);
}
