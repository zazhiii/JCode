package com.zazhi.core.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

public final class PowerShellExecutor {

    private static final int TIMEOUT_SECONDS = 120;
    private static final int MAX_OUTPUT_LENGTH = 50_000;

    private static final List<String> DANGEROUS_COMMANDS = List.of(
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

    private PowerShellExecutor() {
    }

    /**
     * 在当前工作目录运行 PowerShell 命令。
     */
    public static String runPowerShell(String command) {
        if (command == null || command.isBlank()) {
            return "Error: Command is empty";
        }

        if (isDangerous(command)) {
            return "Error: Dangerous command blocked";
        }

        Process process = null;
        ExecutorService outputReader = Executors.newSingleThreadExecutor();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "powershell.exe",
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                command
            );

            // 等价于 Python 中的 cwd=os.getcwd()
            File workingDirectory =
                new File(System.getProperty("user.dir"));

            processBuilder.directory(workingDirectory);

            // 将标准错误合并到标准输出
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();

            Process runningProcess = process;

            // 异步读取，避免命令输出过多导致进程阻塞
            Future<String> outputFuture = outputReader.submit(() ->
                new String(
                    runningProcess.getInputStream().readAllBytes(),
                    Charset.defaultCharset()
                )
            );

            boolean finished = process.waitFor(
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            );

            if (!finished) {
                killProcessTree(process);
                return "Error: Timeout (120s)";
            }

            String output;

            try {
                output = outputFuture.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                outputFuture.cancel(true);
                return "Error: Failed to read command output";
            }

            output = output.strip();

            if (output.isEmpty()) {
                return "(no output)";
            }

            if (output.length() > MAX_OUTPUT_LENGTH) {
                return output.substring(0, MAX_OUTPUT_LENGTH);
            }

            return output;

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

            return "Error: " + (
                cause != null
                    ? cause.getMessage()
                    : e.getMessage()
            );

        } finally {
            outputReader.shutdownNow();
        }
    }

    /**
     * 检查危险命令，忽略大小写。
     */
    private static boolean isDangerous(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);

        return DANGEROUS_COMMANDS.stream()
            .anyMatch(normalized::contains);
    }

    /**
     * 超时时终止 PowerShell 及其子进程。
     */
    private static void killProcessTree(Process process) {
        ProcessHandle processHandle = process.toHandle();

        processHandle.descendants()
            .forEach(child -> {
                try {
                    child.destroyForcibly();
                } catch (Exception ignored) {
                    // 子进程可能已经结束
                }
            });

        processHandle.destroyForcibly();
    }

    public static void main(String[] args) {
        String command;

        if (args.length == 0) {
            command = "Get-ChildItem | Select-Object Name, Length";
        } else {
            command = String.join(" ", args);
        }

        String result = runPowerShell(command);
        System.out.println(result);
    }
}