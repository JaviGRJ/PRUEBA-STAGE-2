package org.ulpgc.bigdata.stage2;

import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import java.net.URI;
import java.net.http.*;
import java.util.Map;

public final class ControlService {
    public static void main(String[] args) {
        Stage2Config c = Stage2Config.fromEnvironment("control-service", 7000);
        String ingestion = System.getenv().getOrDefault("INGESTION_URL", "http://localhost:7001");
        String indexing = System.getenv().getOrDefault("INDEXING_URL", "http://localhost:7002");
        Javalin app = Javalin.create(config -> { config.http.defaultContentType = "application/json"; config.jsonMapper(new JavalinGson()); }).start(c.port());
        app.get("/status", ctx -> ctx.json(Map.of("service", c.serviceName(), "status", "running")));
        app.post("/run/{bookId}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("bookId"));
            HttpClient client = HttpClient.newHttpClient();
            call(client, ingestion + "/ingest/" + id, "POST");
            call(client, indexing + "/index/update/" + id, "POST");
            ctx.json(Map.of("book_id", id, "status", "indexed"));
        });
        app.exception(Exception.class, (e, ctx) -> ctx.status(502).json(Map.of("error", e.getMessage())));
    }
    private static void call(HttpClient client, String url, String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).method(method, HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("workflow call failed: " + response.body());
    }
}
