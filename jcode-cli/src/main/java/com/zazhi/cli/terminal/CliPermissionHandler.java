package com.zazhi.cli.terminal;

import com.zazhi.cli.PermissionMode;
import com.zazhi.core.permission.PermissionDecision;
import com.zazhi.core.permission.PermissionHandler;
import com.zazhi.core.permission.PermissionRequest;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.function.Function;

public final class CliPermissionHandler implements PermissionHandler {
    private final PermissionMode mode;
    private final Function<String, String> readLine;
    private final PrintWriter out;

    public CliPermissionHandler(PermissionMode mode, Function<String, String> readLine, PrintWriter out) {
        this.mode = mode;
        this.readLine = readLine;
        this.out = out;
    }

    @Override
    public PermissionDecision request(PermissionRequest request) {
        if (mode == PermissionMode.DENY) return PermissionDecision.DENY;
        if (mode == PermissionMode.ACCEPT_EDITS && request.risk() == PermissionRequest.Risk.EDIT) {
            return PermissionDecision.ALLOW;
        }

        out.println();
        out.println("需要权限：" + request.reason());
        out.println(request.toolName() + ": " + request.input());
        out.flush();
        String answer = readLine.apply("允许执行？[y/N] ");
        String normalized = answer == null ? "" : answer.strip().toLowerCase(Locale.ROOT);
        return normalized.equals("y") || normalized.equals("yes")
                ? PermissionDecision.ALLOW
                : PermissionDecision.DENY;
    }
}
