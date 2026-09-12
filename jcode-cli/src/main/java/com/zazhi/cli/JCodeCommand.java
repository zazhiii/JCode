package com.zazhi.cli;

import com.zazhi.cli.command.AskCommand;
import com.zazhi.cli.command.ChatCommand;
import com.zazhi.cli.command.ConfigCommand;
import com.zazhi.core.AgentEngine;
import com.zazhi.core.Config;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "jcode",
        description = "在本地工作区运行 JCode 编码 Agent。",
        mixinStandardHelpOptions = true,
        scope = ScopeType.INHERIT,
        version = "JCode 1.0-SNAPSHOT",
        subcommands = {AskCommand.class, ChatCommand.class, ConfigCommand.class}
)
public final class JCodeCommand implements Callable<Integer> {
    @Option(names = {"-C", "--directory"}, description = "工作目录。", defaultValue = ".", scope = ScopeType.INHERIT)
    private Path directory;

    @Option(names = {"-m", "--model"}, description = "临时覆盖模型 ID。", scope = ScopeType.INHERIT)
    private String model;

    @Option(names = "--permission-mode", description = "权限模式：${COMPLETION-CANDIDATES}", defaultValue = "ASK", scope = ScopeType.INHERIT)
    private PermissionMode permissionMode;

    @Option(names = "--no-color", description = "禁用 ANSI 彩色输出。", scope = ScopeType.INHERIT)
    private boolean noColor;

    @Override
    public Integer call() {
        System.out.println("请使用 jcode ask、jcode chat 或 jcode config。运行 jcode --help 查看帮助。");
        return 0;
    }

    public Path workspace() {
        Path workspace = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(workspace)) {
            throw new IllegalArgumentException("工作目录不存在: " + workspace);
        }
        return workspace;
    }

    public Config config() {
        return Config.load(workspace()).withModelId(model);
    }

    public AgentEngine engine() {
        return new AgentEngine(config());
    }

    public PermissionMode permissionMode() {
        return permissionMode;
    }

    public boolean colorEnabled() {
        return !noColor && System.console() != null;
    }
}
