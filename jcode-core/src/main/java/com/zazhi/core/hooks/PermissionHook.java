package com.zazhi.core.hooks;

import com.anthropic.models.messages.ToolUseBlock;
import com.zazhi.core.permission.PermissionDecision;
import com.zazhi.core.permission.PermissionHandler;
import com.zazhi.core.permission.ToolPermissionPolicy;


/**
 * @author zazhi
 * @date 2026/9/5
 * @description:
 */
public class PermissionHook implements PreToolUseHookCallback {
    private final PermissionHandler permissionHandler;

    public PermissionHook(PermissionHandler permissionHandler) {
        this.permissionHandler = permissionHandler;
    }
    @Override
    public String onPreToolUse(ToolUseBlock block) {
        return ToolPermissionPolicy.evaluate(block)
                .filter(request -> permissionHandler.request(request) == PermissionDecision.DENY)
                .map(request -> "Permission denied by user")
                .orElse(null);
    }
}
