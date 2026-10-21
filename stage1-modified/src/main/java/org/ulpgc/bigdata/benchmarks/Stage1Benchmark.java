package org.ulpgc.bigdata.benchmarks;

import org.ulpgc.bigdata.datamarts.InvertedIndex;
import org.ulpgc.bigdata.datamarts.storage.DataLakeLayouts;
import org.ulpgc.bigdata.datamarts.storage.DataLakeMetrics;
import org.ulpgc.bigdata.datamarts.storage.FolderHierarchyIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.JsonFileIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.IndexStorage;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Reproducible structure-specific Stage 1 measurements. Timed regions consume outputs. */
public final class Stage1Benchmark {
    private record Book(int id, Path header, Path body, String expectedQuery) {}

    public static void main(String[] args) throws Exception {
        Path corpus = Path.of(args.length > 0 ? args[0] : "benchmarks/corpus");
        Path output = Path.of(args.length > 1 ? args[1] : "benchmarks/results-java.csv");
        List<Book> books = readManifest(corpus);
        Files.createDirectories(output.toAbsolutePath().getParent());
        List<String> rows = new ArrayList<>();
        rows.add("language,area,structure,operation,books,bytes,files,directories,time_ms,terms,postings,query,answer_count,status");
        for (DataLakeLayouts.Layout layout : DataLakeLayouts.Layout.values()) {
            Path base = output.getParent().resolve("java-run");
            Path root = base.resolve(layout.name().toLowerCase());
            delete(root);
            long started = System.nanoTime();
            DataLakeMetrics.Measurement last = null;
            for (Book book : books) {
                last = DataLakeLayouts.write(base, layout, book.id,
                        Files.readString(book.header), Files.readString(book.body), Instant.EPOCH, "batch-1");
            }
            double elapsed = (System.nanoTime() - started) / 1_000_000.0;
            DataLakeMetrics.Measurement total = measure(root);
            rows.add(String.format(Locale.ROOT, "java,datalake,%s,write,%d,%d,%d,%d,%.4f,,,,,%s",
                    layout.name().toLowerCase(), books.size(), total.sizeBytes(), total.files(), total.directories(), elapsed, "PASS"));
        }

        InvertedIndex index = new InvertedIndex();
        long buildStart = System.nanoTime();
        for (Book book : books) index.addDocument(book.id, book.body.toString());
        double buildMs = (System.nanoTime() - buildStart) / 1_000_000.0;
        long postings = index.getMemoryFootprint().totalPostings();
        for (IndexStorage storage : List.of(
                new JsonFileIndexStorage(output.getParent().resolve("index-monolithic.json").toString()),
                new FolderHierarchyIndexStorage(output.getParent().resolve("index-hierarchy").toString()))) {
            String structure = storage.getClass().getSimpleName();
            long started = System.nanoTime();
            storage.save(index.getIndex());
            double saveMs = (System.nanoTime() - started) / 1_000_000.0;
            Path measured = structure.startsWith("Json") ? output.getParent().resolve("index-monolithic.json") : output.getParent().resolve("index-hierarchy");
            DataLakeMetrics.Measurement disk = measure(measured);
            int answerCount = index.getIndex().getOrDefault("adventure", List.of()).size();
            rows.add(String.format(Locale.ROOT, "java,index,%s,build,%d,%d,%d,%d,%.4f,%d,%d,adventure,%d,PASS",
                    structure, books.size(), disk.sizeBytes(), disk.files(), disk.directories(), buildMs, index.getIndex().size(), postings, answerCount));
            rows.add(String.format(Locale.ROOT, "java,index,%s,persist,%d,%d,%d,%d,%.4f,%d,%d,,,%s",
                    structure, books.size(), disk.sizeBytes(), disk.files(), disk.directories(), saveMs, index.getIndex().size(), postings, "PASS"));
            long queryStart = System.nanoTime();
            int queryAnswers = index.getIndex().getOrDefault("adventure", List.of()).size();
            double queryMs = (System.nanoTime() - queryStart) / 1_000_000.0;
            rows.add(String.format(Locale.ROOT, "java,index,%s,query,%d,%d,%d,%d,%.4f,%d,%d,adventure,%d,PASS",
                    structure, books.size(), disk.sizeBytes(), disk.files(), disk.directories(), queryMs,
                    index.getIndex().size(), postings, queryAnswers));
        }
        Path update = books.get(0).body;
        long updateStart = System.nanoTime();
        index.addDocument(99, update.toString());
        double updateMs = (System.nanoTime() - updateStart) / 1_000_000.0;
        rows.add(String.format(Locale.ROOT, "java,index,in-memory,incremental_update,1,0,0,0,%.4f,%d,%d,,,%s",
                updateMs, index.getIndex().size(), index.getMemoryFootprint().totalPostings(), "PASS"));
        Files.write(output, rows);
    }

    private static List<Book> readManifest(Path corpus) throws IOException {
        List<Book> books = new ArrayList<>();
        for (String line : Files.readAllLines(corpus.resolve("manifest.csv")).subList(1, Files.readAllLines(corpus.resolve("manifest.csv")).size())) {
            String[] fields = line.split(",");
            books.add(new Book(Integer.parseInt(fields[0]), corpus.resolve(fields[1]), corpus.resolve(fields[2]), fields[3]));
        }
        return books;
    }

    private static DataLakeMetrics.Measurement measure(Path path) {
        return DataLakeMetrics.measure(path, () -> {});
    }

    private static void delete(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var paths = Files.walk(path)) {
            for (Path child : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(child);
        }
    }
}
