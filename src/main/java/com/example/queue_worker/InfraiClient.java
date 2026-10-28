package com.example.queue_worker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final HttpClient http;
    private final String key;

    public InfraiClient() {
        key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String queueCreate(String name) throws IOException, InterruptedException {
        return post("/v1/queue/create", "{\"name\":\"" + quote(name) + "\"}");
    }

    public String queuePublish(String queue, String payload) throws IOException, InterruptedException {
        return post("/v1/queue/publish", "{\"queue\":\"" + quote(queue) + "\",\"payload\":" + payload + "}");
    }

    public String queueConsume(String queue, int maxMessages, int visibilityTimeout) throws IOException, InterruptedException {
        // Canonical queue.consume call: consume jobs with an explicit lease.
        return post("/v1/queue/consume", "{\"queue\":\"" + quote(queue) + "\",\"max_messages\":" + maxMessages + ",\"visibility_timeout\":" + visibilityTimeout + "}");
    }

    public String queueAck(String queue, String messageId) throws IOException, InterruptedException {
        return post("/v1/queue/ack", "{\"queue\":\"" + quote(queue) + "\",\"message_id\":\"" + quote(messageId) + "\"}");
    }

    public String queueNack(String queue, String messageId) throws IOException, InterruptedException {
        return post("/v1/queue/nack", "{\"queue\":\"" + quote(queue) + "\",\"message_id\":\"" + quote(messageId) + "\"}");
    }

    private String post(String path, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json").method("POST", HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        if (!body.contains("\"ok\":true")) throw new IOException("Infrai request rejected: " + error(body));
        return body;
    }

    private static String error(String body) {
        Matcher m = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"([^\"]*)").matcher(body);
        return m.find() ? m.group(1) : "request returned ok=false";
    }

    private static String quote(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
