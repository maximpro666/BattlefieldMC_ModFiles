package com.yourmod.teamsystem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourmod.teamsystem.TeamSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConfigManager {
    private static final Path BASE = Path.of("config", "teamsystem");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <T> T load(String relativePath, Class<T> clazz, Supplier<T> defaults) {
        Path file = BASE.resolve(relativePath);
        if (Files.exists(file)) {
            try {
                T parsed = GSON.fromJson(Files.readString(file), clazz);
                if (parsed != null) return parsed;
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                TeamSystem.LOGGER.error("ConfigManager: failed to load {}: {}", relativePath, e.getMessage());
            }
        }
        T def = defaults.get();
        save(relativePath, def);
        return def;
    }

    public static <T> void save(String relativePath, T obj) {
        Path file = BASE.resolve(relativePath);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(obj));
        } catch (IOException e) {
            TeamSystem.LOGGER.error("ConfigManager: failed to save {}: {}", relativePath, e.getMessage());
        }
    }

    public static <T> List<T> loadAll(String directory, Class<T> elementClass) {
        Path dir = BASE.resolve(directory);
        if (!Files.exists(dir)) return List.of();
        List<T> results = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(".json")).collect(Collectors.toList());
            for (Path file : files) {
                try {
                    T parsed = GSON.fromJson(Files.readString(file), elementClass);
                    if (parsed != null) results.add(parsed);
                } catch (IOException | com.google.gson.JsonSyntaxException e) {
                    TeamSystem.LOGGER.error("ConfigManager: failed to load {}: {}", file.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            TeamSystem.LOGGER.error("ConfigManager: failed to list {}: {}", directory, e.getMessage());
        }
        return results;
    }

    public static Path getBasePath() {
        return BASE;
    }

    public static Gson getGson() {
        return GSON;
    }
}
