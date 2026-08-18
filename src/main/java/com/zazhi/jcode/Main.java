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
    public static void main(String[] args) throws IOException, InterruptedException {
        App.launch(App.class, args);
    }
}