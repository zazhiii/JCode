package com.zazhi.jcode.tools;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolUnion;

import java.util.List;
import java.util.Map;

/**
 * @author zazhi
 * @date 2026/8/24
 * @description:
 */
public final class ToolDefinitions {

    private ToolDefinitions() {
    }

    private static JsonValue stringProperty() {
        return JsonValue.from(Map.of("type", "string"));
    }

    private static JsonValue integerProperty() {
        return JsonValue.from(Map.of(
                "type", "integer",
                "minimum", 0
        ));
    }

    private static Tool createTool(
            String name,
            String description,
            Map<String, JsonValue> properties,
            List<String> required
    ) {
        Tool.InputSchema.Properties.Builder propertyBuilder =
                Tool.InputSchema.Properties.builder();

        properties.forEach(
                propertyBuilder::putAdditionalProperty
        );

        return Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(
                        Tool.InputSchema.builder()
                                .type(JsonValue.from("object"))
                                .properties(propertyBuilder.build())
                                .required(required)
                                .build()
                )
                .build();
    }

    public static final Tool POWERSHELL = createTool(
            "powershell",
            "Run a PowerShell command in the workspace.",
            Map.of("command", stringProperty()),
            List.of("command")
    );

    public static final Tool READ_FILE = createTool(
            "read_file",
            "Read a UTF-8 text file from the workspace.",
            Map.of(
                    "path", stringProperty(),
                    "limit", integerProperty()
            ),
            List.of("path")
    );

    public static final Tool WRITE_FILE = createTool(
            "write_file",
            "Write UTF-8 content to a file in the workspace.",
            Map.of(
                    "path", stringProperty(),
                    "content", stringProperty()
            ),
            List.of("path", "content")
    );

    public static final Tool EDIT_FILE = createTool(
            "edit_file",
            "Replace the first exact occurrence of text in a file.",
            Map.of(
                    "path", stringProperty(),
                    "old_text", stringProperty(),
                    "new_text", stringProperty()
            ),
            List.of("path", "old_text", "new_text")
    );

    public static final Tool GLOB = createTool(
            "glob",
            "Find workspace files matching a glob pattern.",
            Map.of("pattern", stringProperty()),
            List.of("pattern")
    );

    public static final List<ToolUnion> ALL = List.of(
            ToolUnion.ofTool(POWERSHELL),
            ToolUnion.ofTool(READ_FILE),
            ToolUnion.ofTool(WRITE_FILE),
            ToolUnion.ofTool(EDIT_FILE),
            ToolUnion.ofTool(GLOB)
    );
}
