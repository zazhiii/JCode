package com.zazhi.core;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.zazhi.core.permission.PermissionHandler;

import java.nio.file.Path;
import java.util.Objects;

public final class AgentEngine {
    private final Config config;
    private final AnthropicClient client;

    public AgentEngine(Config config) {
        this.config = Objects.requireNonNull(config, "config");
        config.requireValid();
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(config.getApiKey())
                .baseUrl(stripTrailingSlash(config.getBaseUrl()))
                .build();
    }

    public AgentSession createSession(
            Path workspace,
            PermissionHandler permissionHandler,
            AgentListener listener
    ) {
        return new AgentSession(
                client,
                config,
                workspace,
                Objects.requireNonNull(permissionHandler, "permissionHandler"),
                listener == null ? AgentListener.noop() : listener
        );
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
