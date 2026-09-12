package com.zazhi.core.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author zazhi
 * @date 2026/8/24
 * @description:
 */
public class WorkspaceTools {

    // System.getProperty("user.dir")返回的是执行运行Java程序命令的路径。
    private static final Path WORKDIR = Path.of(
            System.getProperty("user.dir")
    ).toAbsolutePath().normalize();

    private static final int MAX_OUTPUT_LENGTH = 50_000;

    private WorkspaceTools() {
    }

    private static Path safePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path is empty");
        }

        Path resolved = WORKDIR.resolve(path)
                .toAbsolutePath()
                .normalize();

        if (!resolved.startsWith(WORKDIR)) {
            throw new IllegalArgumentException(
                    "Path escapes workspace: " + path
            );
        }

        return resolved;
    }

    public static String readFile(String path, Integer limit) {
        try {
            List<String> lines = Files.readAllLines(
                    safePath(path),
                    StandardCharsets.UTF_8
            );

            if (limit != null && limit >= 0 && limit < lines.size()) {
                int remaining = lines.size() - limit;

                return String.join("\n", lines.subList(0, limit))
                        + "\n... (" + remaining + " more lines)";
            }

            return truncate(String.join("\n", lines));
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String writeFile(String path, String content) {
        try {
            if (content == null) {
                return "Error: Content is null";
            }

            Path file = safePath(path);
            Path parent = file.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    file,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            return "Wrote " + content.length() + " characters to " + path;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String editFile(
            String path,
            String oldText,
            String newText
    ) {
        try {
            Path file = safePath(path);
            String text = Files.readString(file, StandardCharsets.UTF_8);

            int position = text.indexOf(oldText);

            if (position < 0) {
                return "Error: Text not found in " + path;
            }

            String result =
                    text.substring(0, position)
                    + newText
                    + text.substring(position + oldText.length());

            Files.writeString(
                    file,
                    result,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            return "Edited " + path;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String glob(String pattern) {
        try {
            if (pattern == null || pattern.isBlank()) {
                return "Error: Glob pattern is empty";
            }

            PathMatcher matcher = FileSystems.getDefault()
                    .getPathMatcher("glob:" + pattern);

            try (Stream<Path> paths = Files.walk(WORKDIR)) {
                String result = paths
                        .filter(Files::isRegularFile)
                        .map(WORKDIR::relativize)
                        .filter(matcher::matches)
                        .map(Path::toString)
                        .sorted()
                        .limit(1_000)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("(no matches)");

                return truncate(result);
            }
        } catch (IOException | RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }


    private static String truncate(String output) {
        if (output.length() <= MAX_OUTPUT_LENGTH) {
            return output;
        }

        return output.substring(0, MAX_OUTPUT_LENGTH)
                + "\n... (output truncated)";
    }
}
