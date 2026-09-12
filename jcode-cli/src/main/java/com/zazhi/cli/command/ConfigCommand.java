package com.zazhi.cli.command;

import com.zazhi.cli.JCodeCommand;
import com.zazhi.core.Config;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "config",
        description = "检查 JCode 配置。",
        mixinStandardHelpOptions = true,
        subcommands = {ConfigCommand.Show.class, ConfigCommand.Doctor.class}
)
public final class ConfigCommand implements Callable<Integer> {
    @ParentCommand
    private JCodeCommand parent;

    @Override
    public Integer call() {
        System.out.println("请使用 config show 或 config doctor。");
        return 0;
    }

    @Command(name = "show", description = "显示当前有效配置。")
    static final class Show implements Callable<Integer> {
        @ParentCommand
        private ConfigCommand command;

        @Override
        public Integer call() {
            Config config = command.parent.config();
            System.out.println("workspace = " + command.parent.workspace());
            System.out.println("llm.base_url = " + value(config.getBaseUrl()));
            System.out.println("llm.model_id = " + value(config.getModelId()));
            System.out.println("llm.api_key = " + (blank(config.getApiKey()) ? "(未配置)" : "(已配置)"));
            return 0;
        }
    }

    @Command(name = "doctor", description = "验证运行 CLI 所需的配置。")
    static final class Doctor implements Callable<Integer> {
        @ParentCommand
        private ConfigCommand command;

        @Override
        public Integer call() {
            Config config = command.parent.config();
            List<String> errors = config.validate();
            if (errors.isEmpty()) {
                System.out.println("配置有效。Workspace: " + command.parent.workspace());
                return 0;
            }
            errors.forEach(error -> System.err.println("- " + error));
            return 3;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String value(String value) {
        return blank(value) ? "(未配置)" : value;
    }
}
