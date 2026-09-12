package com.zazhi.cli.command;

import com.zazhi.cli.JCodeCommand;
import com.zazhi.cli.terminal.CliPermissionHandler;
import com.zazhi.cli.terminal.TerminalRenderer;
import com.zazhi.core.AgentListener;
import com.zazhi.core.AgentSession;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "ask", description = "执行一次任务并在完成后退出。", mixinStandardHelpOptions = true)
public final class AskCommand implements Callable<Integer> {
    @ParentCommand
    private JCodeCommand parent;

    @Parameters(arity = "1..*", paramLabel = "PROMPT", description = "发送给 Agent 的任务。")
    private List<String> prompt;

    @Option(names = "--json", description = "仅将最终结果以 JSON 写入标准输出。")
    private boolean json;

    @Override
    public Integer call() {
        PrintWriter out = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        PrintWriter err = new PrintWriter(System.err, true, StandardCharsets.UTF_8);
        TerminalRenderer renderer = new TerminalRenderer(err, parent.colorEnabled());
        AgentListener listener = json ? AgentListener.noop() : renderer;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        AgentSession session = parent.engine().createSession(
                parent.workspace(),
                new CliPermissionHandler(parent.permissionMode(), value -> readLine(input, err, value), err),
                listener
        );
        String response = session.submit(String.join(" ", prompt));
        if (json) {
            out.println("{\"status\":\"ok\",\"response\":\"" + escapeJson(response) + "\"}");
        } else {
            out.println(response);
        }
        return 0;
    }

    private static String readLine(BufferedReader reader, PrintWriter output, String prompt) {
        output.print(prompt);
        output.flush();
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new IllegalStateException("读取终端输入失败", e);
        }
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) escaped.append(String.format("\\u%04x", (int) character));
                    else escaped.append(character);
                }
            }
        }
        return escaped.toString();
    }
}
