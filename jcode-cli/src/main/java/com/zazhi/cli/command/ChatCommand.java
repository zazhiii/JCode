package com.zazhi.cli.command;

import com.zazhi.cli.JCodeCommand;
import com.zazhi.cli.terminal.CliPermissionHandler;
import com.zazhi.cli.terminal.TerminalRenderer;
import com.zazhi.core.AgentSession;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "chat", description = "启动交互式会话。", mixinStandardHelpOptions = true)
public final class ChatCommand implements Callable<Integer> {
    @ParentCommand
    private JCodeCommand parent;

    @Override
    public Integer call() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            Path stateDirectory = Path.of(System.getProperty("user.home"), ".jcode");
            Files.createDirectories(stateDirectory);
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .appName("jcode")
                    .variable(LineReader.HISTORY_FILE, stateDirectory.resolve("history"))
                    .option(LineReader.Option.HISTORY_INCREMENTAL, true)
                    .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                    .build();

            TerminalRenderer renderer = new TerminalRenderer(terminal.writer(), parent.colorEnabled());
            AgentSession session = parent.engine().createSession(
                    parent.workspace(),
                    new CliPermissionHandler(parent.permissionMode(), reader::readLine, terminal.writer()),
                    renderer
            );

            renderer.heading("JCode · " + parent.workspace());
            terminal.writer().println("Model: " + parent.config().getModelId());
            terminal.writer().println("输入 /help 查看命令，Ctrl+D 退出。\n");
            terminal.writer().flush();

            while (true) {
                try {
                    String line = reader.readLine("You > ").strip();
                    if (line.isEmpty()) continue;
                    if (line.equals("/exit") || line.equals("/quit")) break;
                    if (line.equals("/help")) {
                        terminal.writer().println("/help  /clear  /new  /model  /workspace  /exit");
                        continue;
                    }
                    if (line.equals("/clear") || line.equals("/new")) {
                        session.clear();
                        terminal.writer().println("已开始新会话。\n");
                        continue;
                    }
                    if (line.equals("/model")) {
                        terminal.writer().println(parent.config().getModelId());
                        continue;
                    }
                    if (line.equals("/workspace")) {
                        terminal.writer().println(parent.workspace());
                        continue;
                    }
                    String response = session.submit(line);
                    terminal.writer().println("\nJCode > " + response + "\n");
                    terminal.writer().flush();
                } catch (UserInterruptException ignored) {
                    terminal.writer().println("^C");
                } catch (EndOfFileException ignored) {
                    break;
                }
            }
        }
        return 0;
    }
}
