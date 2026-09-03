package com.zazhi.jcode;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * LLM 配置，持久化到 {@code ~/.jcode/config.toml}，每次 {@link #load()} 从磁盘读取。
 */
public final class Config {
    private static final Path CONFIG_DIR =
            Path.of(System.getProperty("user.home"), ".jcode");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.toml");

    private final String baseUrl;
    private final String apiKey;
    private final String modelId;

    public Config(String baseUrl, String apiKey, String modelId) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelId = modelId == null ? "" : modelId.trim();
    }

    public static Path configDir() {
        return CONFIG_DIR;
    }

    public static Path configFile() {
        return CONFIG_FILE;
    }

    /**
     * 每次调用都从用户目录 {@code ~/.jcode/config.toml} 重新加载。
     */
    public static Config load() {
        ensureConfigFile();
        try (FileConfig fileConfig = FileConfig.builder(CONFIG_FILE.toFile())
                .sync()
                .build()) {
            fileConfig.load();
            return new Config(
                    fileConfig.get("llm.base_url"),
                    fileConfig.get("llm.api_key"),
                    fileConfig.get("llm.model_id")
            );
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建配置目录: " + CONFIG_DIR, e);
        }

        try (FileConfig fileConfig = FileConfig.builder(CONFIG_FILE.toFile())
                .sync()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            fileConfig.set("llm.base_url", baseUrl);
            fileConfig.set("llm.api_key", apiKey);
            fileConfig.set("llm.model_id", modelId);
            fileConfig.save();
        }
    }

    public void requireApiKey() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "请先在「设置 → LLM 配置」中填写 llm.api_key。"
            );
        }
    }

    private static void ensureConfigFile() {
        if (Files.exists(CONFIG_FILE)) {
            return;
        }
        try {
            Files.createDirectories(CONFIG_DIR);
            Config migrated = tryMigrateFromClasspathProperties();
            if (migrated != null) {
                migrated.save();
                return;
            }
            new Config(
                    "https://api.deepseek.com/anthropic",
                    "",
                    "deepseek-v4-flash"
            ).save();
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化配置文件: " + CONFIG_FILE, e);
        }
    }

    private static Config tryMigrateFromClasspathProperties() {
        try (InputStream input = Config.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(input);
            String apiKey = properties.getProperty("llm.api_key");
            if (apiKey == null || apiKey.isBlank()
                    || apiKey.contains("xxxxxxxx")) {
                return null;
            }
            return new Config(
                    properties.getProperty("llm.base_url"),
                    apiKey,
                    properties.getProperty("llm.model_id")
            );
        } catch (IOException ignored) {
            return null;
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Config config)) {
            return false;
        }
        return Objects.equals(baseUrl, config.baseUrl)
                && Objects.equals(apiKey, config.apiKey)
                && Objects.equals(modelId, config.modelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUrl, apiKey, modelId);
    }
}
