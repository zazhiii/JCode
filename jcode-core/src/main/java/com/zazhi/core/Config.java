package com.zazhi.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Config {
    private final String baseUrl;
    private final String apiKey;
    private final String modelId;

    public Config() {
        this(load(Path.of(System.getProperty("user.dir"))));
    }

    private Config(Config config) {
        this(config.baseUrl, config.apiKey, config.modelId);
    }

    public Config(String baseUrl, String apiKey, String modelId) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelId = modelId;
    }

    public static Config load(Path workspace) {
        Properties properties = new Properties();
        // 用户目录下的配置文件
        loadProperties(properties, Path.of(System.getProperty("user.home"), ".jcode", "config.properties"));
        // 项目目录下的配置文件
        loadProperties(properties, workspace.toAbsolutePath().normalize().resolve(".jcode/config.properties"));

        return new Config(
                environmentOrProperty("LLM_BASE_URL", properties, "llm.base_url"),
                environmentOrProperty("LLM_API_KEY", properties, "llm.api_key"),
                environmentOrProperty("LLM_MODEL_ID", properties, "llm.model_id")
        );
    }

    private static void loadProperties(Properties properties, Path path) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("读取配置失败: " + path, e);
        }
    }

    public Config withModelId(String override) {
        return override == null || override.isBlank()
                ? this
                : new Config(baseUrl, apiKey, override);
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (baseUrl == null || baseUrl.isBlank()) errors.add("缺少 LLM_BASE_URL / llm.base_url");
        if (apiKey == null || apiKey.isBlank()) errors.add("缺少 LLM_API_KEY / llm.api_key");
        if (modelId == null || modelId.isBlank()) errors.add("缺少 LLM_MODEL_ID / llm.model_id");
        return List.copyOf(errors);
    }

    public void requireValid() {
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join("; ", errors));
        }
    }

    private static String environmentOrProperty(
            String environmentName,
            Properties properties,
            String propertyName
    ) {
        String environmentValue = System.getenv(environmentName);
        return environmentValue == null || environmentValue.isBlank()
                ? properties.getProperty(propertyName)
                : environmentValue;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelId() {
        return modelId;
    }
}
