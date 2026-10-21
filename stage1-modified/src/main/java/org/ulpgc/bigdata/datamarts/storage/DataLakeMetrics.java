package org.ulpgc.bigdata.datamarts.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Reproducible byte and millisecond measurements for by_book/by_time/by_batch layouts. */
public final class DataLakeMetrics {
    private DataLakeMetrics() {}

    public static Measurement measure(Path root, Runnable operation) {
        long started = System.nanoTime();
        operation.run();
        long bytes = 0;
        try (Stream<Path> files = Files.exists(root) ? Files.walk(root) : Stream.empty()) {
            for (Path path : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) bytes += Files.size(path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to measure datalake " + root, e);
        }
        return new Measurement(bytes, bytes / 1024.0, (System.nanoTime() - started) / 1_000_000.0);
    }

    public record Measurement(long sizeBytes, double sizeKb, double timeMs) {}
}
