package com.pigeostudios.pwp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pigeostudios.pwp.PWP;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ConfigManager {
    private static final Path BASE = Path.of("config", "pwp");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <T> T load(String relativePath, Class<T> clazz, Supplier<T> defaults) {
        Path file = BASE.resolve(relativePath);
        if (Files.exists(file)) {
            try {
                T parsed = GSON.fromJson(Files.readString(file), clazz);
                if (parsed != null) return parsed;
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                PWP.LOGGER.error("ConfigManager: failed to load {}: {}", relativePath, e.getMessage());
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
            PWP.LOGGER.error("ConfigManager: failed to save {}: {}", relativePath, e.getMessage());
        }
    }

    public static <T> List<T> loadAll(String directory, Class<T> elementClass) {
        Path dir = BASE.resolve(directory);
        if (!Files.exists(dir) || isEmpty(dir)) {
            extractDefaults(directory);
        }
        List<T> results = new ArrayList<>();
        if (Files.exists(dir)) {
            try (var stream = Files.list(dir)) {
                List<Path> files = stream.filter(p -> p.toString().endsWith(".json")).collect(Collectors.toList());
                for (Path file : files) {
                    try {
                        T parsed = GSON.fromJson(Files.readString(file), elementClass);
                        if (parsed != null) results.add(parsed);
                    } catch (IOException | com.google.gson.JsonSyntaxException e) {
                        PWP.LOGGER.error("ConfigManager: failed to load {}: {}", file.getFileName(), e.getMessage());
                    }
                }
            } catch (IOException e) {
                PWP.LOGGER.error("ConfigManager: failed to list {}: {}", directory, e.getMessage());
            }
        }
        if (results.isEmpty()) {
            PWP.LOGGER.warn("No files found on disk for '{}', trying classpath fallback...", directory);
            results.addAll(loadAllFromClasspath(directory, elementClass));
        }
        return results;
    }

    private static <T> List<T> loadAllFromClasspath(String directory, Class<T> elementClass) {
        List<T> results = new ArrayList<>();
        String prefix = "/config/pwp/" + directory.replace('\\', '/') + "/";
        try {
            List<String> bundledFiles = discoverBundledFiles(prefix);
            if (bundledFiles.isEmpty()) {
                PWP.LOGGER.warn("No bundled config files found on classpath for: {}", prefix);
                return results;
            }
            for (String fileName : bundledFiles) {
                try (InputStream is = ConfigManager.class.getResourceAsStream(prefix + fileName)) {
                    if (is == null) {
                        PWP.LOGGER.warn("Failed to open resource: {}{}", prefix, fileName);
                        continue;
                    }
                    String content = new String(is.readAllBytes());
                    T parsed = GSON.fromJson(content, elementClass);
                    if (parsed != null) results.add(parsed);
                } catch (IOException | com.google.gson.JsonSyntaxException e) {
                    PWP.LOGGER.error("ConfigManager: failed to load {} from classpath: {}", fileName, e.getMessage());
                }
            }
            PWP.LOGGER.info("Loaded {} definitions from classpath for '{}'", results.size(), directory);
        } catch (Exception e) {
            PWP.LOGGER.error("ConfigManager: failed to discover bundled files for {}: {}", directory, e.getMessage());
        }
        return results;
    }

    private static void extractDefaults(String directory) {
        Path outDir = BASE.resolve(directory);
        try {
            Files.createDirectories(outDir);
            String prefix = "/config/pwp/" + directory.replace('\\', '/') + "/";
            List<String> bundledFiles = discoverBundledFiles(prefix);
            if (bundledFiles.isEmpty()) {
                PWP.LOGGER.warn("No bundled config files found for: {}", prefix);
            }
            for (String fileName : bundledFiles) {
                Path outFile = outDir.resolve(fileName);
                if (Files.exists(outFile)) continue;
                try (InputStream is = ConfigManager.class.getResourceAsStream(prefix + fileName)) {
                    if (is != null) {
                        Files.copy(is, outFile);
                        PWP.LOGGER.info("Extracted default config: {}", outFile);
                    } else {
                        PWP.LOGGER.warn("Bundled config not found: {}{}", prefix, fileName);
                    }
                }
            }
        } catch (IOException e) {
            PWP.LOGGER.error("ConfigManager: failed to extract defaults for {}: {}", directory, e.getMessage());
        }
    }

    private static List<String> discoverBundledFiles(String prefix) {
        String normalizedPrefix = prefix.startsWith("/") ? prefix.substring(1) : prefix;
        if (!normalizedPrefix.endsWith("/")) normalizedPrefix += "/";

        // Strategy 1: Class.getResource (works in standard Java)
        try {
            URL url = ConfigManager.class.getResource(prefix);
            if (url != null) return listFilesFromUrl(url, prefix);
        } catch (Exception ignored) {}

        // Strategy 2: Find the JAR / classpath root containing this class
        try {
            URL location = ConfigManager.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) return List.of();
            URI locationUri = location.toURI();
            Path locationPath = Path.of(locationUri);

            if (locationPath.toString().endsWith(".jar")) {
                // Production — mod is deployed as a JAR
                return listFilesFromJar(locationPath, normalizedPrefix);
            } else if (Files.isDirectory(locationPath)) {
                // Dev environment — try filesystem paths
                List<String> files = listFilesFromDevFs(locationPath, normalizedPrefix);
                if (!files.isEmpty()) return files;
            }
        } catch (Exception ignored) {}

        // Strategy 3: fallback — relative to CWD
        List<String> files = listFilesFromDir(Path.of(normalizedPrefix.replace('/', File.separatorChar)));
        if (!files.isEmpty()) return files;

        PWP.LOGGER.warn("discoverBundledFiles: no bundled files found for {}", prefix);
        return List.of();
    }

    private static List<String> listFilesFromUrl(URL url, String prefix) throws Exception {
        List<String> files = new ArrayList<>();
        URI uri = url.toURI();
        Path dirPath;
        if ("jar".equals(uri.getScheme())) {
            FileSystem fs;
            try {
                fs = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                fs = FileSystems.newFileSystem(uri, Map.of());
            }
            dirPath = fs.getPath(prefix);
        } else {
            dirPath = Path.of(uri);
        }
        try (var stream = Files.list(dirPath)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString())
                .sorted()
                .forEach(files::add);
        }
        return files;
    }

    private static List<String> listFilesFromJar(Path jarPath, String normalizedPrefix) throws IOException {
        List<String> files = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(normalizedPrefix) && name.endsWith(".json") && !name.equals(normalizedPrefix)) {
                    String fileName = name.substring(normalizedPrefix.length());
                    if (!fileName.contains("/") && !fileName.isEmpty()) {
                        files.add(fileName);
                    }
                }
            }
            Collections.sort(files);
        }
        return files;
    }

    private static List<String> listFilesFromDevFs(Path locationPath, String normalizedPrefix) {
        // locationPath is typically build/classes/java/main/
        Path buildDir = locationPath.getParent().getParent();   // build/
        Path resourceDir = buildDir.resolve("resources").resolve("main").resolve(normalizedPrefix.replace('/', File.separatorChar));
        List<String> files = listFilesFromDir(resourceDir);
        if (!files.isEmpty()) return files;
        // Try relative to CWD as last resort for dev
        return listFilesFromDir(Path.of(normalizedPrefix.replace('/', File.separatorChar)));
    }

    private static List<String> listFilesFromDir(Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return List.of();
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private static boolean isEmpty(Path dir) {
        try (var stream = Files.list(dir)) {
            return !stream.findAny().isPresent();
        } catch (IOException e) {
            return true;
        }
    }

    public static Path getBasePath() {
        return BASE;
    }

    public static Gson getGson() {
        return GSON;
    }
}
