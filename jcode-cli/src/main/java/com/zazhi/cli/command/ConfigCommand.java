package com.zazhi.cli.command;

import com.zazhi.cli.JCodeCommand;
import com.zazhi.core.ConfigStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(name = "config", description = "查看或修改 JCode 的 LLM 配置。",
        mixinStandardHelpOptions = true,
        subcommands = {ConfigCommand.Show.class, ConfigCommand.Doctor.class,
                ConfigCommand.Init.class, ConfigCommand.Set.class, ConfigCommand.Unset.class})
public final class ConfigCommand implements Callable<Integer> {
    @ParentCommand private JCodeCommand parent;

    @Override public Integer call() {
        System.out.println("请使用 config init、set、unset、show 或 doctor。");
        return 0;
    }

    @Command(name = "show", description = "显示当前有效配置及来源。")
    static final class Show implements Callable<Integer> {
        @ParentCommand private ConfigCommand command;

        @Override public Integer call() {
            ConfigStore.Config config = new ConfigStore().load(command.parent.workspace());
            System.out.println("workspace = " + command.parent.workspace());
            System.out.println("llm.base_url = " + display(config.baseUrl()) + source(config, "llm.base_url"));
            System.out.println("llm.model_id = " + display(config.modelId()) + source(config, "llm.model_id"));
            System.out.println("llm.api_key = " + (blank(config.apiKey()) ? "(未配置)" : "(已配置)")
                    + source(config, "llm.api_key"));
            return 0;
        }
    }

    @Command(name = "doctor", description = "验证运行 CLI 所需的配置。")
    static final class Doctor implements Callable<Integer> {
        @ParentCommand private ConfigCommand command;

        @Override public Integer call() {
            List<String> errors = command.parent.config().validate();
            if (errors.isEmpty()) {
                System.out.println("配置有效。Workspace: " + command.parent.workspace());
                return 0;
            }
            errors.forEach(error -> System.err.println("- " + error));
            System.err.println("运行 jcode config init，或设置 LLM_BASE_URL、LLM_API_KEY、LLM_MODEL_ID。");
            return 3;
        }
    }

    @Command(name = "init", description = "交互式配置 LLM，默认保存到用户目录。")
    static final class Init implements Callable<Integer> {
        @ParentCommand private ConfigCommand command;
        @Option(names = "--scope", defaultValue = "user", description = "保存位置：${COMPLETION-CANDIDATES}")
        private ConfigStore.Scope scope;

        @Override public Integer call() {
            runInit(command.parent, scope);
            return 0;
        }
    }

    @Command(name = "set", description = "设置 llm.base_url、llm.model_id 或 llm.api_key。")
    static final class Set implements Callable<Integer> {
        @ParentCommand private ConfigCommand command;
        @Parameters(index = "0", description = "配置项") private String key;
        @Parameters(index = "1", arity = "0..1", description = "非密钥配置值") private String value;
        @Option(names = "--scope", defaultValue = "user", description = "保存位置：${COMPLETION-CANDIDATES}")
        private ConfigStore.Scope scope;
        @Option(names = "--stdin", description = "从标准输入读取 API Key。") private boolean stdin;

        @Override public Integer call() throws Exception {
            String setting;
            if ("llm.api_key".equals(key)) {
                if (value != null) throw new IllegalArgumentException("API Key 不可作为命令参数；请使用 --stdin 或交互输入");
                if (stdin) {
                    setting = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
                } else {
                    Console console = requireConsole();
                    char[] secret = console.readPassword("API Key: ");
                    setting = secret == null ? null : new String(secret);
                    if (secret != null) java.util.Arrays.fill(secret, '\0');
                }
            } else {
                if (stdin) throw new IllegalArgumentException("--stdin 仅用于 llm.api_key");
                setting = value;
            }
            ConfigStore store = new ConfigStore();
            store.set(command.parent.workspace(), scope, key, setting);
            System.out.println("已保存 " + key + " 到 " + store.scopePath(command.parent.workspace(), scope));
            warnOverride(store, key);
            return 0;
        }
    }

    @Command(name = "unset", description = "删除指定位置的配置项。")
    static final class Unset implements Callable<Integer> {
        @ParentCommand private ConfigCommand command;
        @Parameters(index = "0", description = "配置项") private String key;
        @Option(names = "--scope", defaultValue = "user", description = "删除位置：${COMPLETION-CANDIDATES}")
        private ConfigStore.Scope scope;

        @Override public Integer call() {
            ConfigStore store = new ConfigStore();
            store.unset(command.parent.workspace(), scope, key);
            System.out.println("已从 " + store.scopePath(command.parent.workspace(), scope) + " 删除 " + key);
            return 0;
        }
    }

    public static void runInit(JCodeCommand parent, ConfigStore.Scope scope) {
        Console console = requireConsole();
        ConfigStore store = new ConfigStore();
        Properties layer = store.readLayer(parent.workspace(), scope);
        console.printf("配置 Anthropic Messages API 或兼容接口。保存位置: %s%n", store.scopePath(parent.workspace(), scope));
        String baseUrl = ask(console, "API 服务地址", layer.getProperty("llm.base_url"));
        String modelId = ask(console, "模型 ID", layer.getProperty("llm.model_id"));
        String oldKey = layer.getProperty("llm.api_key");
        char[] entered = console.readPassword("API Key%s: ", blank(oldKey) ? "" : " [回车保留原值]");
        if (entered == null) throw new IllegalStateException("已取消配置");
        String apiKey = entered.length == 0 ? oldKey : new String(entered);
        java.util.Arrays.fill(entered, '\0');
        if (blank(apiKey)) throw new IllegalArgumentException("API Key 不能为空");
        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("llm.base_url", baseUrl);
        updates.put("llm.model_id", modelId);
        updates.put("llm.api_key", apiKey);
        store.setAll(parent.workspace(), scope, updates);
        console.printf("配置已保存。%n");
        updates.keySet().forEach(key -> warnOverride(store, key));
        if (scope == ConfigStore.Scope.project) console.printf("项目配置包含凭据，请勿提交到版本库。%n");
    }

    private static String ask(Console console, String label, String previous) {
        String input = console.readLine("%s%s: ", label, blank(previous) ? "" : " [回车保留原值]");
        if (input == null) throw new IllegalStateException("已取消配置");
        String result = input.isBlank() ? previous : input.strip();
        if (blank(result)) throw new IllegalArgumentException(label + "不能为空");
        return result;
    }

    private static Console requireConsole() {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("需要交互终端；非交互环境请使用环境变量或 config set");
        return console;
    }

    private static void warnOverride(ConfigStore store, String key) {
        String variable = store.environmentOverride(key);
        if (variable != null) System.err.println("提示: 当前 " + key + " 由环境变量 " + variable + " 覆盖。");
    }

    private static String source(ConfigStore.Config config, String key) {
        return " [来源: " + config.source(key) + "]";
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String display(String value) { return blank(value) ? "(未配置)" : value; }
}
