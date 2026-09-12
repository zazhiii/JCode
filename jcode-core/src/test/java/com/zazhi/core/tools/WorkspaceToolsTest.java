package com.zazhi.core.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceToolsTest {
    @TempDir
    Path workspace;

    @Test
    void readsAndWritesInsideWorkspace() throws Exception {
        WorkspaceTools tools = new WorkspaceTools(workspace);

        assertTrue(tools.writeFile("src/demo.txt", "hello").startsWith("Wrote"));
        assertEquals("hello", tools.readFile("src/demo.txt", null));
        assertEquals("hello", Files.readString(workspace.resolve("src/demo.txt")));
    }

    @Test
    void rejectsPathsOutsideWorkspace() {
        WorkspaceTools tools = new WorkspaceTools(workspace);

        assertTrue(tools.readFile("../secret.txt", null).startsWith("Error: Path escapes workspace"));
        assertTrue(tools.writeFile("../secret.txt", "nope").startsWith("Error: Path escapes workspace"));
    }
}
