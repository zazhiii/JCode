package com.zazhi.core.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShellExecutorTest {
    @TempDir
    Path workspace;

    @Test
    void detectsSupportedPlatforms() {
        assertEquals(ShellExecutor.Platform.WINDOWS, ShellExecutor.Platform.detect("Windows 11"));
        assertEquals(ShellExecutor.Platform.MACOS, ShellExecutor.Platform.detect("Mac OS X"));
        assertEquals(ShellExecutor.Platform.MACOS, ShellExecutor.Platform.detect("Darwin"));
        assertEquals(ShellExecutor.Platform.UNSUPPORTED, ShellExecutor.Platform.detect("Linux"));
        assertEquals(ShellExecutor.Platform.UNSUPPORTED, ShellExecutor.Platform.detect(null));
    }

    @Test
    void buildsPlatformSpecificCommandLines() {
        assertEquals(
                List.of("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "Get-Location"),
                ShellExecutor.commandLine(ShellExecutor.Platform.WINDOWS, "Get-Location")
        );
        assertEquals(
                List.of("/bin/zsh", "-lc", "pwd"),
                ShellExecutor.commandLine(ShellExecutor.Platform.MACOS, "pwd")
        );
    }

    @Test
    void rejectsEmptyAndUnsupportedCommands() {
        ShellExecutor mac = executor(ShellExecutor.Platform.MACOS);
        ShellExecutor unsupported = executor(ShellExecutor.Platform.UNSUPPORTED);

        assertEquals("Error: Command is empty", mac.run("  "));
        assertTrue(unsupported.run("pwd").startsWith("Error: Unsupported operating system:"));
    }

    @Test
    void executesCommandsInWorkspaceOnMacOs() throws Exception {
        ShellExecutor executor = executor(ShellExecutor.Platform.MACOS);

        assertEquals(workspace.toRealPath().toString(), Path.of(executor.run("pwd")).toRealPath().toString());
        assertEquals("hello", executor.run("printf hello"));
        assertEquals("(no output)", executor.run("true"));
    }

    @Test
    void reportsNonZeroExitCodes() {
        String result = executor(ShellExecutor.Platform.MACOS).run("printf failure; exit 7");

        assertTrue(result.startsWith("Error: Command exited with code 7"));
        assertTrue(result.contains("failure"));
    }

    @Test
    void truncatesLongOutput() {
        ShellExecutor executor = new ShellExecutor(workspace, ShellExecutor.Platform.MACOS, 5, 5);

        assertEquals("12345", executor.run("printf 123456789"));
    }

    @Test
    void terminatesCommandsThatExceedTimeout() {
        ShellExecutor executor = new ShellExecutor(workspace, ShellExecutor.Platform.MACOS, 1, 100);

        assertEquals("Error: Timeout (1s)", executor.run("sleep 5"));
    }

    @Test
    void blocksCatastrophicCommandsForEachPlatform() {
        assertTrue(ShellExecutor.isDangerous("Remove-Item -Recurse .", ShellExecutor.Platform.WINDOWS));
        assertTrue(ShellExecutor.isDangerous("rm -rf /", ShellExecutor.Platform.MACOS));
        assertTrue(ShellExecutor.isDangerous("sudo rm -r -f -- /", ShellExecutor.Platform.MACOS));
        assertTrue(ShellExecutor.isDangerous("diskutil eraseDisk APFS Empty /dev/disk2", ShellExecutor.Platform.MACOS));
        assertFalse(ShellExecutor.isDangerous("ls -la", ShellExecutor.Platform.MACOS));
        assertFalse(ShellExecutor.isDangerous("Get-ChildItem", ShellExecutor.Platform.WINDOWS));
    }

    private ShellExecutor executor(ShellExecutor.Platform platform) {
        return new ShellExecutor(workspace, platform, 5, ShellExecutor.DEFAULT_MAX_OUTPUT_LENGTH);
    }
}
