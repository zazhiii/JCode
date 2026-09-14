package com.zazhi.core.permission;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.DirectCaller;
import com.anthropic.models.messages.ToolUseBlock;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolPermissionPolicyTest {
    @Test
    void requestsPermissionForWindowsAndPosixMutations() {
        assertDestructive("Remove-Item demo.txt");
        assertDestructive("rm demo.txt");
        assertDestructive("rmdir empty-directory");
        assertDestructive("mv old.txt new.txt");
        assertDestructive("chmod 600 secret.txt");
        assertDestructive("diskutil eraseDisk APFS Empty /dev/disk2");
    }

    @Test
    void allowsReadOnlyCommandsWithoutPrompt() {
        assertTrue(ToolPermissionPolicy.evaluate(shell("ls -la")).isEmpty());
        assertTrue(ToolPermissionPolicy.evaluate(shell("Get-ChildItem")).isEmpty());
    }

    @Test
    void stillRequestsPermissionForFileTools() {
        PermissionRequest request = ToolPermissionPolicy.evaluate(tool(
                "write_file", Map.of("path", "demo.txt", "content", "hello"))).orElseThrow();

        assertEquals(PermissionRequest.Risk.EDIT, request.risk());
    }

    private static void assertDestructive(String command) {
        PermissionRequest request = ToolPermissionPolicy.evaluate(shell(command)).orElseThrow();
        assertEquals(PermissionRequest.Risk.DESTRUCTIVE, request.risk());
    }

    private static ToolUseBlock shell(String command) {
        return tool("shell", Map.of("command", command));
    }

    private static ToolUseBlock tool(String name, Map<String, String> input) {
        return ToolUseBlock.builder()
                .id("tool-1")
                .name(name)
                .caller(DirectCaller.builder().build())
                .input(JsonValue.from(input))
                .build();
    }
}
