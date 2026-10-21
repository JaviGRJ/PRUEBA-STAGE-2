package org.ulpgc.bigdata.control;

import org.ulpgc.bigdata.datamarts.storage.DataLakeLayouts;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/** Downloads one Gutenberg book, splits Header/Body, writes all layouts, then updates control state. */
public final class GutenbergIngestion {
    private final Path datalakeRoot;
    private final PipelineController controller;
    private final HttpClient client;

    public GutenbergIngestion(Path datalakeRoot, PipelineController controller) {
        this.datalakeRoot = datalakeRoot;
        this.controller = controller;
        this.client = HttpClient.newHttpClient();
    }

    public void download(int bookId, String batch) {
        if (bookId <= 0) throw new IllegalArgumentException("bookId must be positive");
        if (controller.isDownloaded(bookId)) return;
        try {
            String url = "https://www.gutenberg.org/files/" + bookId + "/" + bookId + "-0.txt";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("Gutenberg returned HTTP " + response.statusCode());
            }
            String raw = response.body();
            int marker = raw.indexOf("*** START OF");
            String header = marker >= 0 ? raw.substring(0, marker) : raw.substring(0, Math.min(raw.length(), 8192));
            String body = marker >= 0 ? raw.substring(marker) : raw;
            Instant downloadedAt = Instant.now();
            for (DataLakeLayouts.Layout layout : DataLakeLayouts.Layout.values()) {
                DataLakeLayouts.write(datalakeRoot, layout, bookId, header, body, downloadedAt, batch);
            }
            controller.markDownloaded(bookId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to download Gutenberg book " + bookId, e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to download Gutenberg book " + bookId, e);
        }
    }
}
