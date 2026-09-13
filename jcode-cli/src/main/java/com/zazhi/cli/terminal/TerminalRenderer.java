package com.zazhi.cli.terminal;

import com.zazhi.core.AgentEvent;
import com.zazhi.core.AgentListener;
import com.zazhi.core.permission.PermissionDecision;

import java.io.PrintWriter;

public final class TerminalRenderer implements AgentListener {
    private final PrintWriter progress;
    private final boolean color;

    public TerminalRenderer(PrintWriter progress, boolean color) {
        this.progress = progress;
        this.color = color;
    }

    @Override
    public void onEvent(AgentEvent event) {
        switch (event) {
            case AgentEvent.ToolStarted started -> updateLine("● 正在执行 " + started.name(), "36");
            case AgentEvent.ToolFinished finished -> commitLine(
                    (finished.error() ? "✗ " : "✓ ") + finished.name(),
                    finished.error() ? "31" : "32"
            );
            case AgentEvent.PermissionResolved resolved -> commitLine(
                    "权限: " + resolved.decision().name().toLowerCase(),
                    resolved.decision() == PermissionDecision.ALLOW ? "33" : "31");
            case AgentEvent.Failed failed -> commitLine("✗ " + failed.error().getMessage(), "31");
            default -> {
            }
        }
    }

    public void heading(String text) {
        commitLine(text, "1;36");
    }

    private void commitLine(String text, String code) {
        progress.print("\r\033[2K");
        String res = color
                ? "\033[" + code + "m" + text + "\033[0m"
                : text;
        progress.println(res);
        progress.flush();
    }

    private void updateLine(String text, String code) {
        String res = color
                ? "\033[" + code + "m" + text + "\033[0m"
                : text;
        progress.print("\r\033[2K");
        progress.print(res);
        progress.flush();
    }

    private static String compact(String value) {
        String compact = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 117) + "...";
    }
}
