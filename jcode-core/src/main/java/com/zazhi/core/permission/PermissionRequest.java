package com.zazhi.core.permission;

public record PermissionRequest(String toolName, String input, Risk risk, String reason) {
    public enum Risk {
        EDIT,
        DESTRUCTIVE
    }
}
