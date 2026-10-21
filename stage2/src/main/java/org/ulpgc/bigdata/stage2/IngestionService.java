package org.ulpgc.bigdata.stage2;

import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;

public final class IngestionService {
    public static void main(String[] args) {
        Stage2Config config = Stage2Config.fromEnvironment("ingestion-service", 7001);
        Path root = config.dataRoot().resolve("datalake/by_book");
        Path control = config.dataRoot().resolve("control/downloaded_books.txt");
        Javalin app = Javalin.create(c -> { c.http.defaultContentType = "application/json"; c.jsonMapper(new JavalinGson()); }).start(config.port());
        app.get("/status", ctx -> ctx.json(Map.of("service", config.serviceName(), "status", "running")));
        app.post("/ingest/{bookId}", ctx -> {
            int id = parseId(ctx.pathParam("bookId"));
            Path bookDir = root.resolve(String.valueOf(id));
            Files.createDirectories(bookDir);
            Path body = bookDir.resolve(id + ".body.txt");
            if (!Files.exists(body) || Files.size(body) == 0) {
                String url = "https://www.gutenberg.org/files/" + id + "/" + id + "-0.txt";
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) throw new IOException("Gutenberg returned HTTP " + response.statusCode());
                String raw = response.body();
                int marker = raw.indexOf("*** START OF");
                String header = marker >= 0 ? raw.substring(0, marker) : raw.substring(0, Math.min(raw.length(), 8192));
                String content = marker >= 0 ? raw.substring(marker) : raw;
                Files.writeString(bookDir.resolve(id + ".header.txt"), header, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Files.writeString(body, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            appendUnique(control, id);
            ctx.status(200).json(Map.of("book_id", id, "status", "downloaded", "body", body.toString()));
        });
        app.exception(Exception.class, (e, ctx) -> ctx.status(500).json(Map.of("error", e.getMessage())));
    }

    private static int parseId(String value) { int id = Integer.parseInt(value); if (id <= 0) throw new IllegalArgumentException("bookId must be positive"); return id; }
    private static synchronized void appendUnique(Path path, int id) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Set<String> ids = Files.exists(path) ? new HashSet<>(Files.readAllLines(path)) : new HashSet<>();
        if (ids.add(String.valueOf(id))) Files.writeString(path, id + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
