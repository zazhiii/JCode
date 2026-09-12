package com.zazhi.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private final String baseUrl;
    private final String apiKey;
    private final String modelId;

    public Config() {
        Properties properties = loadProperties();
        this.baseUrl = environmentOrProperty("LLM_BASE_URL", properties, "llm.base_url");
        this.apiKey = environmentOrProperty("LLM_API_KEY", properties, "llm.api_key");
        this.modelId = environmentOrProperty("LLM_MODEL_ID", properties, "llm.model_id");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先设置环境变量 LLM_API_KEY");
        }
    }

    public Config(String baseUrl, String apiKey, String modelId) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelId = modelId;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("读取 config.properties 失败", e);
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