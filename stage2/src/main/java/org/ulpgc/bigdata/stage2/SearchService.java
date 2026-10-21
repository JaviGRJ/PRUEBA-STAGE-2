package org.ulpgc.bigdata.stage2;

import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public final class SearchService {
    public static void main(String[] args) {
        Stage2Config config = Stage2Config.fromEnvironment("search-service", 7003);
        Path data = config.dataRoot().resolve("datamart");
        Javalin app = Javalin.create(c -> { c.http.defaultContentType = "application/json"; c.jsonMapper(new JavalinGson()); }).start(config.port());
        app.get("/status", ctx -> ctx.json(Map.of("service", config.serviceName(), "status", "running")));
        app.get("/search", ctx -> {
            String query = Optional.ofNullable(ctx.queryParam("q")).orElse("").trim().toLowerCase(Locale.ROOT);
            if (query.isBlank()) { ctx.status(400).json(Map.of("error", "q is required")); return; }
            int offset = nonNegative(ctx.queryParam("offset"), 0), limit = Math.min(nonNegative(ctx.queryParam("limit"), 20), 100);
            String author = ctx.queryParam("author"), language = ctx.queryParam("language"), year = ctx.queryParam("year");
            PublishedSnapshot snapshot = readSnapshot(data.resolve("published.json"));
            Map<String, List<Integer>> index = snapshot.index();
            Map<String, Book> books = snapshot.metadata();
            List<Book> matches = new ArrayList<>();
            for (Integer id : index.getOrDefault(query, List.of())) {
                Book book = books.get(String.valueOf(id));
                if (book != null && matches(book, author, language, year)) matches.add(book);
            }
            int total = matches.size();
            List<Book> page = matches.stream().skip(offset).limit(limit).collect(Collectors.toList());
            Map<String, String> filters = new LinkedHashMap<>(); if (author != null) filters.put("author", author); if (language != null) filters.put("language", language); if (year != null) filters.put("year", year);
            ctx.json(Map.of("query", query, "filters", filters, "total", total, "offset", offset, "limit", limit, "results", page));
        });
        app.get("/books/{bookId}", ctx -> {
            Map<String, Book> books = readSnapshot(data.resolve("published.json")).metadata();
            Book book = books.get(ctx.pathParam("bookId")); if (book == null) { ctx.status(404).json(Map.of("error", "book not found")); return; } ctx.json(book);
        });
        app.exception(Exception.class, (e, ctx) -> ctx.status(500).json(Map.of("error", e.getMessage())));
    }
    private static boolean matches(Book b, String author, String language, String year) { return (author == null || b.author().toLowerCase(Locale.ROOT).contains(author.toLowerCase(Locale.ROOT))) && (language == null || b.language().equalsIgnoreCase(language)) && (year == null || String.valueOf(b.year()).equals(year)); }
    private static int nonNegative(String s, int fallback) { try { int v = s == null ? fallback : Integer.parseInt(s); if (v < 0) throw new IllegalArgumentException("pagination must be non-negative"); return v; } catch (NumberFormatException e) { throw new IllegalArgumentException("pagination must be numeric"); } }
    private static <T> T read(Path path, java.lang.reflect.Type type) { try { return Files.exists(path) ? JsonFiles.GSON.fromJson(Files.readString(path), type) : JsonFiles.GSON.fromJson("{}", type); } catch (Exception e) { throw new IllegalStateException("cannot read published index", e); } }
    private static PublishedSnapshot readSnapshot(Path path) {
        try {
            if (!Files.exists(path)) return new PublishedSnapshot(new HashMap<>(), new HashMap<>());
            return JsonFiles.GSON.fromJson(Files.readString(path), PublishedSnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("cannot read published index", e);
        }
    }
}
