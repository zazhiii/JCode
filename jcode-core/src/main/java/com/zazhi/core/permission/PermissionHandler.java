package com.zazhi.core.permission;

@FunctionalInterface
public interface PermissionHandler {
    PermissionDecision request(PermissionRequest request);

    static PermissionHandler denyAll() {
        return request -> PermissionDecision.DENY;
    }

    static PermissionHandler allowAll() {
        return request -> PermissionDecision.ALLOW;
    }
}
