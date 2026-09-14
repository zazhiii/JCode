package com.zazhi.core.tools;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.DirectCaller;
import com.anthropic.models.messages.ToolUseBlock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolDispatcherTest {
    @TempDir
    Path workspace;

    @Test
    void dispatchesShellTool() {
        ShellExecutor shellExecutor = new ShellExecutor(
                workspace, ShellExecutor.Platform.MACOS, 5, ShellExecutor.DEFAULT_MAX_OUTPUT_LENGTH);
        ToolDispatcher.ToolExecution execution = new ToolDispatcher(workspace, shellExecutor)
                .execute(tool("shell", Map.of("command", "printf hello")));

        assertFalse(execution.error());
        assertEquals("hello", execution.output());
    }

    @Test
    void rejectsFormerPowerShellToolName() {
        ShellExecutor shellExecutor = new ShellExecutor(
                workspace, ShellExecutor.Platform.MACOS, 5, ShellExecutor.DEFAULT_MAX_OUTPUT_LENGTH);
        ToolDispatcher.ToolExecution execution = new ToolDispatcher(workspace, shellExecutor)
                .execute(tool("powershell", Map.of("command", "printf hello")));

        assertTrue(execution.error());
        assertEquals("Error: Unknown tool: powershell", execution.output());
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
