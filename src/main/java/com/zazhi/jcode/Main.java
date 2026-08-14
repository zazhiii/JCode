package com.zazhi.jcode;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

//import static jdk.internal.util.OperatingSystem.isWindows;

public class Main {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final Path WORKING_DIRECTORY = Path.of("")
            .toAbsolutePath()
            .normalize();

    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_RESET = "\033[0m";

    private static final Config CONFIG = new Config();

//    private static final String SHELL_NAME =
//            isWindows() ? "Windows cmd" : "bash";

    private static final String SHELL_NAME = "Windows cmd";


    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("工作目录：" + WORKING_DIRECTORY);
        System.out.println("输入问题，回车发送。输入 q 退出。\n");

        List<Map<String, String>> history = new ArrayList<>();

        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                System.out.print(ANSI_CYAN + "s01 >> " + ANSI_RESET);

                if (!scanner.hasNextLine()) {
                    break;
                }

                String query = scanner.nextLine().trim();

                if (query.isEmpty()
                        || query.equalsIgnoreCase("q")
                        || query.equalsIgnoreCase("exit")) {
                    break;
                }
                history.add(Map.of("role", "user", "content", query));

                agentLoop(history);

                Map<String, String> lastMsg = history.getLast();
                System.out.println(ANSI_YELLOW + "s01 << " + ANSI_RESET + lastMsg.get("content") + "\n");
            }

        }

    }

    private static void agentLoop(List<Map<String, String>> messages) throws IOException, InterruptedException {
        while (true) {
            String resp = callLLM(messages);
            JSONObject jsonObject = JSON.parseObject(resp);
            JSONObject choices = jsonObject.getJSONArray("choices").getJSONObject(0);
            String msg = choices.getJSONObject("message").getString("content");
            String finishReason = choices.getString("finish_reason");
            messages.add(Map.of("role", "assistant", "content", msg));

            if (!Objects.equals(finishReason, "tool_use")) {
                return;
            }

            // TODO: tool_use
        }
    }

    private static String callLLM(List<Map<String, String>> messages) throws IOException, InterruptedException {

        String requestBody = JSON.toJSONString(Map.of(
                "model", CONFIG.getModelId(),
                "messages", messages)
        );

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(stripTrailingSlash(CONFIG.getBaseUrl()) + "/chat/completions"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + CONFIG.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            if (!isRetryable(response.statusCode()) || attempt == MAX_ATTEMPTS) {
                throw apiException(response);
            }

            long delayMillis = retryDelayMillis(response, attempt);
            System.err.printf(
                    "API 暂时不可用（HTTP %d），%d ms 后进行第 %d/%d 次尝试%n",
                    response.statusCode(), delayMillis, attempt + 1, MAX_ATTEMPTS
            );
            Thread.sleep(delayMillis);
        }

        return null;
    }


//    private static String callLLM(String baseUrl, String apiKey, String model)
//            throws IOException, InterruptedException {
//        String requestBody = JSON.toJSONString(Map.of(
//                "model", model,
//                "messages", List.of(Map.of("role", "user", "content", "你好"))
//        ));
//
//        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(stripTrailingSlash(baseUrl) + "/chat/completions"))
//                    .timeout(REQUEST_TIMEOUT)
//                    .header("Authorization", "Bearer " + apiKey)
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
//                    .build();
//
//            HttpResponse<String> response = HTTP_CLIENT.send(
//                    request,
//                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
//            );
//
//            if (response.statusCode() >= 200 && response.statusCode() < 300) {
//                return response.body();
//            }
//
//            if (!isRetryable(response.statusCode()) || attempt == MAX_ATTEMPTS) {
//                throw apiException(response);
//            }
//
//            long delayMillis = retryDelayMillis(response, attempt);
//            System.err.printf(
//                    "API 暂时不可用（HTTP %d），%d ms 后进行第 %d/%d 次尝试%n",
//                    response.statusCode(), delayMillis, attempt + 1, MAX_ATTEMPTS
//            );
//            Thread.sleep(delayMillis);
//        }
//
//        throw new IllegalStateException("Unreachable");
//    }

    private static boolean isRetryable(int statusCode) {
        return statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 503
                || statusCode == 504;
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        if (retryAfter != null) {
            try {
                return Math.min(Long.parseLong(retryAfter) * 1_000L, 30_000L);
            } catch (NumberFormatException ignored) {
                // Retry-After 也可能是 HTTP 日期；无法直接解析时使用指数退避。
            }
        }

        long exponentialDelay = 1_000L * (1L << (attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(250L, 751L);
        return Math.min(exponentialDelay + jitter, 30_000L);
    }

    private static IOException apiException(HttpResponse<String> response) {
        return new IOException(
                "API 请求失败，HTTP " + response.statusCode() + System.lineSeparator() + response.body()
        );
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}