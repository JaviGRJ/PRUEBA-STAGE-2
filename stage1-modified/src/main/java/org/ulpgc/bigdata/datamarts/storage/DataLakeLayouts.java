package org.ulpgc.bigdata.datamarts.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Writes the three Stage 1 datalake layouts and returns reproducible metrics. */
public final class DataLakeLayouts {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private DataLakeLayouts() {}

    public enum Layout { BY_BOOK, BY_TIME, BY_BATCH }

    public static DataLakeMetrics.Measurement write(
            Path root, Layout layout, int bookId, String header, String body, Instant downloadedAt, String batch) {
        Path layoutRoot = root.resolve(layout.name().toLowerCase());
        return DataLakeMetrics.measure(layoutRoot, () -> {
            try {
                Path directory = switch (layout) {
                    case BY_BOOK -> layoutRoot.resolve(String.valueOf(bookId));
                    case BY_TIME -> layoutRoot.resolve(DAY.format(downloadedAt));
                    case BY_BATCH -> layoutRoot.resolve(batch == null || batch.isBlank() ? "default" : batch);
                };
                Files.createDirectories(directory);
                Files.writeString(directory.resolve(bookId + ".header.txt"), header);
                Files.writeString(directory.resolve(bookId + ".body.txt"), body);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to write " + layout + " datalake layout", e);
            }
        });
    }
}
