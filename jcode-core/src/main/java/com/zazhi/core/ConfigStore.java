package com.zazhi.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.*;
import java.util.*;
import java.net.URI;

public class ConfigStore {
    public enum Scope {user, project}

    public record Config(String baseUrl, String apiKey, String modelId, Map<String, String> sources) {
        public List<String> validate() {
            List<String> errors = new ArrayList<>();
            if (baseUrl == null || baseUrl.isBlank()) errors.add("缺少 LLM_BASE_URL / llm.base_url");
            if (apiKey == null || apiKey.isBlank()) errors.add("缺少 LLM_API_KEY / llm.api_key");
            if (modelId == null || modelId.isBlank()) errors.add("缺少 LLM_MODEL_ID / llm.model_id");
            if (baseUrl != null && !baseUrl.isBlank()) {
                try {
                    URI uri = URI.create(baseUrl);
                    if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                            || uri.getHost() == null) errors.add("llm.base_url 必须是 HTTP(S) URL");
                } catch (IllegalArgumentException e) {
                    errors.add("llm.base_url 必须是 HTTP(S) URL");
                }
            }
            return List.copyOf(errors);
        }

        public String source(String key) {
            return sources.get(key);
        }
    }

    private Path home;
    private Map<String, String> environment;

    private static final Map<String, String> ENV_NAMES = Map.of(
            "llm.base_url", "LLM_BASE_URL",
            "llm.api_key", "LLM_API_KEY",
            "llm.model_id", "LLM_MODEL_ID"
    );

    public ConfigStore() {
        this.home = Path.of(System.getProperty("user.home"));
        this.environment = System.getenv();
    }

    public Config load(Path workspace) {
        Properties user = read(home.resolve(".jcode/config.properties"));
        Properties project = read(workspace.toAbsolutePath().normalize().resolve(".jcode/config.properties"));

        // 配置值
        Map<String, String> values = new LinkedHashMap<>();
        // 配置来源
        Map<String, String> sources = new LinkedHashMap<>();
        for (String key : ENV_NAMES.keySet()) {
            String value = user.getProperty(key);
            if (value != null) sources.put(key, "user config");
            if (project.containsKey(key)) {
                value = project.getProperty(key);
                sources.put(key, "project config");
            }

            values.put(key, value);
        }
        return new Config(values.get("llm.base_url"), values.get("llm.api_key"), values.get("llm.model_id"), sources);
    }

    /**
     * 读取配置
     *
     * @param path .propertis 文件路径
     * @return
     */
    private static Properties read(Path path) {
        Properties properties = new Properties();
        if (!Files.isRegularFile(path)) return properties;
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("读取配置失败: " + path, e);
        }
    }

    public Properties readLayer(Path workspace, ConfigStore.Scope scope) {
        return read(scopePath(workspace, scope));
    }

    public Path scopePath(Path workspace, Scope scope) {
        return (scope == Scope.user ?
                home : workspace.toAbsolutePath().normalize()).resolve(".jcode/config.properties");
    }

    public void set(Path workspace, Scope scope, String key, String value) {
        setAll(workspace, scope, Collections.singletonMap(key, value));
    }

    public void unset(Path workspace, Scope scope, String key) {
        requireKey(key);
        Path file = scopePath(workspace, scope);
        Properties values = read(file);
        if (values.remove(key) != null) save(file, values);
    }

    public void setAll(Path workspace, Scope scope, Map<String, String> updates) {
        if (updates.isEmpty()) throw new IllegalArgumentException("没有配置项可保存");
        for (Map.Entry<String, String> update : updates.entrySet()) {
            validateValue(update.getKey(), update.getValue());
        }
        Properties values = readLayer(workspace, scope);
        updates.forEach(values::setProperty);
        save(scopePath(workspace, scope), values);
    }

    private static void validateValue(String key, String value) {
        requireKey(key);
        if (value == null || value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("配置值不能为空或包含换行符");
        }
        if ("llm.base_url".equals(key) && !new Config(value, "x", "x", null).validate().isEmpty()) {
            throw new IllegalArgumentException("llm.base_url 必须是 HTTP(S) URL");
        }
    }

    private static void requireKey(String key) {
        if (!ENV_NAMES.containsKey(key)) throw new IllegalArgumentException("不支持的配置项: " + key);
    }

    private static void save(Path path, Properties values) {
        Path temporary = null;
        try {
            boolean directoryExisted = Files.exists(path.getParent());
            Files.createDirectories(path.getParent());
            if (!directoryExisted) restrictDirectory(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "config-", ".tmp");
            restrictFile(temporary);
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "JCode configuration");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
        } catch (IOException e) {
            throw new IllegalStateException("保存配置失败: " + path, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public String environmentOverride(String key) {
        requireKey(key);
        String value = environment.get(ENV_NAMES.get(key));
        return value == null || value.isBlank() ? null : ENV_NAMES.get(key);
    }

    /**
     * 如果当前文件系统支持 POSIX 权限，就把指定目录的权限收紧为“只有当前所有者可以读、写、进入”
     * 也就是 Linux/macOS 中常见的 700 权限。
     * @param path
     * @throws IOException
     */
    private static void restrictDirectory(Path path) throws IOException {
        // path 所在的文件系统是否支持 POSIX 文件权限，POSIX 权限就是 Linux/macOS 中这种：rwxrwxrwx
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
            // OWNER_READ     -> r
            // OWNER_WRITE    -> w
            // OWNER_EXECUTE  -> x
            Files.setPosixFilePermissions(
                    path,
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE
                    )
            );
        }
    }

    private static void restrictFile(Path path) throws IOException {
        if (Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView.class) != null) {
            Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } else {
            AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
            if (acl != null) acl.setAcl(java.util.List.of(AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW).setPrincipal(acl.getOwner())
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
        }
    }
}
