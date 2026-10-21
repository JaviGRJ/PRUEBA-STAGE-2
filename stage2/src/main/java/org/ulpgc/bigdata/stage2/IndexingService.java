package org.ulpgc.bigdata.stage2;

import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public final class IndexingService {
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9\\s]+");
    public static void main(String[] args) {
        Stage2Config config = Stage2Config.fromEnvironment("indexing-service", 7002);
        Path data = config.dataRoot(), indexFile = data.resolve("datamart/index.json"), metadataFile = data.resolve("datamart/metadata.json");
        Javalin app = Javalin.create(c -> { c.http.defaultContentType = "application/json"; c.jsonMapper(new JavalinGson()); }).start(config.port());
        app.get("/status", ctx -> ctx.json(Map.of("service", config.serviceName(), "status", "running")));
        app.post("/index/update/{bookId}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("bookId"));
            Path body = data.resolve("datalake/by_book").resolve(String.valueOf(id)).resolve(id + ".body.txt");
            if (!Files.isRegularFile(body)) { ctx.status(409).json(Map.of("error", "book has not been ingested")); return; }
            Map<String, List<Integer>> index = readIndex(indexFile);
            for (String term : tokenize(Files.readString(body, StandardCharsets.UTF_8))) {
                List<Integer> postings = index.computeIfAbsent(term, ignored -> new ArrayList<>());
                if (!postings.contains(id)) postings.add(id);
            }
            index.values().forEach(ids -> ids.sort(Integer::compareTo));
            JsonFiles.writeAtomically(indexFile, index);
            Map<String, Book> metadata = readMetadata(metadataFile);
            Book book = parseMetadata(data.resolve("datalake/by_book").resolve(String.valueOf(id)).resolve(id + ".header.txt"), id);
            metadata.put(String.valueOf(id), book);
            MetadataRepository.upsert(data.resolve("metadata.db"), book);
            JsonFiles.writeAtomically(metadataFile, metadata);
            appendUnique(data.resolve("control/indexed_books.txt"), id);
            ctx.json(Map.of("book_id", id, "status", "indexed", "terms", index.size()));
        });
        app.exception(Exception.class, (e, ctx) -> ctx.status(500).json(Map.of("error", e.getMessage())));
    }
    private static Set<String> tokenize(String text) { String normalized = NON_WORD.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").trim(); return normalized.isEmpty() ? Set.of() : new HashSet<>(Arrays.asList(normalized.split("\\s+"))); }
    private static Map<String, List<Integer>> readIndex(Path path) throws Exception { if (!Files.exists(path)) return new HashMap<>(); return JsonFiles.GSON.fromJson(Files.readString(path), new TypeToken<Map<String, List<Integer>>>(){}.getType()); }
    private static Map<String, Book> readMetadata(Path path) throws Exception { if (!Files.exists(path)) return new HashMap<>(); return JsonFiles.GSON.fromJson(Files.readString(path), new TypeToken<Map<String, Book>>(){}.getType()); }
    private static Book parseMetadata(Path path, int id) throws Exception {
        String title = "Book " + id, author = "Unknown", language = "unknown";
        if (Files.exists(path)) for (String line : Files.readAllLines(path)) {
            int separator = line.indexOf(':');
            if (separator < 0) continue;
            String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separator + 1).trim();
            if (key.equals("title")) title = value;
            if (key.equals("author")) author = value;
            if (key.equals("language")) language = value;
        }
        return new Book(id, title, author, language, null);
    }
    private static synchronized void appendUnique(Path path, int id) throws Exception { Files.createDirectories(path.toAbsolutePath().getParent()); Set<String> ids = Files.exists(path) ? new HashSet<>(Files.readAllLines(path)) : new HashSet<>(); if (ids.add(String.valueOf(id))) Files.writeString(path, id + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
}
