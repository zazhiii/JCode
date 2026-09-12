package com.zazhi.core.permission;

import com.anthropic.models.messages.ToolUseBlock;
import com.zazhi.core.tools.PowerShellInput;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ToolPermissionPolicy {
    private static final List<String> DESTRUCTIVE_COMMANDS = List.of(
            "remove-item", "rm ", "del ", "erase ", "set-content",
            "clear-content", "move-item", "rename-item", "icacls"
    );

    private ToolPermissionPolicy() {
    }

    public static Optional<PermissionRequest> evaluate(ToolUseBlock toolUse) {
        if (toolUse.name().equals("write_file")) {
            WriteFileInput write = toolUse._input().convert(WriteFileInput.class);
            return Optional.of(new PermissionRequest(
                    toolUse.name(), write.path(), PermissionRequest.Risk.EDIT, "工具将写入工作区文件"));
        }
        if (toolUse.name().equals("edit_file")) {
            EditFileInput edit = toolUse._input().convert(EditFileInput.class);
            return Optional.of(new PermissionRequest(
                    toolUse.name(), edit.path(), PermissionRequest.Risk.EDIT, "工具将编辑工作区文件"));
        }
        if (toolUse.name().equals("powershell")) {
            String command = toolUse._input().convert(PowerShellInput.class).command();
            String normalized = command.toLowerCase(Locale.ROOT);
            if (DESTRUCTIVE_COMMANDS.stream().anyMatch(normalized::contains)) {
                return Optional.of(new PermissionRequest(
                        toolUse.name(), command, PermissionRequest.Risk.DESTRUCTIVE, "命令可能修改或删除文件"));
            }
        }
        return Optional.empty();
    }

    private record WriteFileInput(String path, String content) {
    }

    private record EditFileInput(String path, String old_text, String new_text) {
    }
}
