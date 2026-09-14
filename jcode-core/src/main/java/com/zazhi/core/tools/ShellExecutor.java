package com.zazhi.core.tools;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public final class ShellExecutor {

    static final int DEFAULT_TIMEOUT_SECONDS = 120;
    static final int DEFAULT_MAX_OUTPUT_LENGTH = 50_000;

    private static final List<String> WINDOWS_BLOCKED_COMMANDS = List.of(
            "remove-item -recurse",
            "remove-item -force",
            "format-volume",
            "clear-disk",
            "remove-partition",
            "shutdown",
            "restart-computer",
            "stop-computer",
            "diskpart",
            "rd /s",
            "rmdir /s",
            "del /s",
            "format c:"
    );

    private static final List<String> MACOS_BLOCKED_COMMANDS = List.of(
            "diskutil erase",
            "diskutil partition",
            "shutdown",
            "reboot",
            "halt"
    );

    private static final Pattern MACOS_ROOT_DELETE = Pattern.compile(
            "\\brm\\s+(?=[^;&|]*(?:-[a-z]*r))(?=[^;&|]*(?:-[a-z]*f))[^;&|]*\\s(?:--\\s+)?/(?:\\*|\\s|$)",
            Pattern.CASE_INSENSITIVE
    );

    private final Path workdir;
    private final Platform platform;
    private final int timeoutSeconds;
    private final int maxOutputLength;

    public ShellExecutor(Path workdir) {
        this(workdir, Platform.detect(System.getProperty("os.name")),
                DEFAULT_TIMEOUT_SECONDS, DEFAULT_MAX_OUTPUT_LENGTH);
    }

    ShellExecutor(Path workdir, Platform platform, int timeoutSeconds, int maxOutputLength) {
        this.workdir = workdir.toAbsolutePath().normalize();
        if (!Files.isDirectory(this.workdir)) {
            throw new IllegalArgumentException("Workspace is not a directory: " + this.workdir);
        }
        this.platform = platform;
        this.timeoutSeconds = timeoutSeconds;
        this.maxOutputLength = maxOutputLength;
    }

    /**
     * 在当前工作目录中使用操作系统对应的 shell 执行命令。
     */
    public String run(String command) {
        if (command == null || command.isBlank()) {
            return "Error: Command is empty";
        }
        if (platform == Platform.UNSUPPORTED) {
            return "Error: Unsupported operating system: " + System.getProperty("os.name");
        }
        if (isDangerous(command, platform)) {
            return "Error: Dangerous command blocked";
        }

        Process process = null;
        ExecutorService outputReader = Executors.newSingleThreadExecutor();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandLine(platform, command));
            processBuilder.directory(workdir.toFile());
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            Process runningProcess = process;
            Future<String> outputFuture = outputReader.submit(() -> new String(
                    runningProcess.getInputStream().readAllBytes(),
                    Charset.defaultCharset()
            ));

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                killProcessTree(process);
                outputFuture.cancel(true);
                return "Error: Timeout (" + timeoutSeconds + "s)";
            }

            String output;
            try {
                output = outputFuture.get(5, TimeUnit.SECONDS).strip();
            } catch (TimeoutException e) {
                outputFuture.cancel(true);
                return "Error: Failed to read command output";
            }

            if (output.length() > maxOutputLength) {
                output = output.substring(0, maxOutputLength);
            }
            if (process.exitValue() != 0) {
                String detail = output.isEmpty() ? "" : ":\n" + output;
                return "Error: Command exited with code " + process.exitValue() + detail;
            }
            return output.isEmpty() ? "(no output)" : output;
        } catch (IOException | SecurityException e) {
            return "Error: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                killProcessTree(process);
            }
            return "Error: Command execution interrupted";
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            return "Error: " + (cause != null ? cause.getMessage() : e.getMessage());
        } finally {
            outputReader.shutdownNow();
        }
    }

    static List<String> commandLine(Platform platform, String command) {
        return switch (platform) {
            case WINDOWS -> List.of(
                    "powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", command);
            case MACOS -> List.of("/bin/zsh", "-lc", command);
            case UNSUPPORTED -> throw new IllegalArgumentException("Unsupported operating system");
        };
    }

    static boolean isDangerous(String command, Platform platform) {
        String normalized = command.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
        if (platform == Platform.WINDOWS) {
            return WINDOWS_BLOCKED_COMMANDS.stream().anyMatch(normalized::contains);
        }
        if (platform == Platform.MACOS) {
            if (MACOS_BLOCKED_COMMANDS.stream().anyMatch(normalized::contains)) {
                return true;
            }
            return MACOS_ROOT_DELETE.matcher(normalized).find();
        }
        return false;
    }

    private static void killProcessTree(Process process) {
        ProcessHandle processHandle = process.toHandle();
        processHandle.descendants().forEach(child -> {
            try {
                child.destroyForcibly();
            } catch (Exception ignored) {
                // 子进程可能已经结束。
            }
        });
        processHandle.destroyForcibly();
    }

    public enum Platform {
        WINDOWS,
        MACOS,
        UNSUPPORTED;

        public static Platform detect(String osName) {
            String normalized = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
            if (normalized.contains("mac") || normalized.contains("darwin")) {
                return MACOS;
            }
            if (normalized.contains("win")) {
                return WINDOWS;
            }
            return UNSUPPORTED;
        }

        public String displayName() {
            return switch (this) {
                case WINDOWS -> "Windows PowerShell";
                case MACOS -> "macOS zsh";
                case UNSUPPORTED -> "unsupported shell";
            };
        }
    }
}
