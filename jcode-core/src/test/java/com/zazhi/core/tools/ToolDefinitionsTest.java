package com.zazhi.core.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolDefinitionsTest {
    @Test
    void exposesGenericShellTool() {
        List<String> names = ToolDefinitions.ALL.stream()
                .map(tool -> tool.asTool().name())
                .toList();

        assertEquals(List.of("shell", "read_file", "write_file", "edit_file", "glob"), names);
        assertEquals("shell", ToolDefinitions.SHELL.name());
    }
}
