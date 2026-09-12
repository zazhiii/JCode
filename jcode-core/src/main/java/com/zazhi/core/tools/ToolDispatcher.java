package com.zazhi.core.tools;

import com.anthropic.models.messages.ToolUseBlock;

/**
 * @author zazhi
 * @date 2026/8/24
 * @description: TODO
 */

public final class ToolDispatcher {

    private ToolDispatcher() {
    }

    public static ToolExecution execute(ToolUseBlock toolUse) {
        try {
            String output = switch (toolUse.name()) {
                case "powershell" -> {
                    PowerShellInput input = toolUse._input()
                            .convert(PowerShellInput.class);

                    yield PowerShellExecutor.runPowerShell(input.command());
                }

                case "read_file" -> {
                    ReadFileInput input = toolUse._input()
                            .convert(ReadFileInput.class);

                    yield WorkspaceTools.readFile(
                            input.path(),
                            input.limit()
                    );
                }

                case "write_file" -> {
                    WriteFileInput input = toolUse._input()
                            .convert(WriteFileInput.class);

                    yield WorkspaceTools.writeFile(
                            input.path(),
                            input.content()
                    );
                }

                case "edit_file" -> {
                    EditFileInput input = toolUse._input()
                            .convert(EditFileInput.class);

                    yield WorkspaceTools.editFile(
                            input.path(),
                            input.old_text(),
                            input.new_text()
                    );
                }

                case "glob" -> {
                    GlobInput input = toolUse._input()
                            .convert(GlobInput.class);

                    yield WorkspaceTools.glob(input.pattern());
                }

                default -> "Error: Unknown tool: " + toolUse.name();
            };

            return new ToolExecution(
                    output,
                    output.startsWith("Error:")
            );
        } catch (RuntimeException e) {
            return new ToolExecution(
                    "Error: Invalid tool input: " + e.getMessage(),
                    true
            );
        }
    }

    public record ToolExecution(
            String output,
            boolean error
    ) {
    }

    private record PowerShellInput(String command) {
    }

    private record ReadFileInput(String path, Integer limit) {
    }

    private record WriteFileInput(String path, String content) {
    }

    private record EditFileInput(
            String path,
            String old_text,
            String new_text
    ) {
    }

    private record GlobInput(String pattern) {
    }
}
