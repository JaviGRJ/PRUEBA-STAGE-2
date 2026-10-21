package org.ulpgc.bigdata.stage2;

import java.nio.file.Path;

public record Stage2Config(int port, Path dataRoot, String serviceName) {
    public static Stage2Config fromEnvironment(String serviceName, int defaultPort) {
        return new Stage2Config(
                integer("PORT", defaultPort),
                Path.of(System.getenv().getOrDefault("DATA_ROOT", "data")),
                serviceName);
    }

    private static int integer(String name, int fallback) {
        try { return Integer.parseInt(System.getenv().getOrDefault(name, String.valueOf(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
