package org.ulpgc.bigdata.stage2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;

final class JsonFiles {
    static final Gson GSON = new GsonBuilder().create();
    private JsonFiles() {}

    static void writeAtomically(Path path, Object value) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(value), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
    }

    static void writePublishedSnapshot(Path path, PublishedSnapshot snapshot) throws IOException {
        writeAtomically(path, snapshot);
    }
}
