package com.pigeostudios.pwp.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pigeostudios.pwp.PWP;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class VisualsConfig {
    private static final String CONFIG_FILE = "pwp_visuals.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static VisualsConfig instance;

    public PointVisual capturePoint = new PointVisual();
    public BaseVisual base = new BaseVisual();
    public BorderVisual border = new BorderVisual();
    public RingVisual rings = new RingVisual();
    public BaseRingVisual baseRings = new BaseRingVisual();
    public SquadMarkerVisual squadMarker = new SquadMarkerVisual();

    public static class SquadMarkerVisual {
        public boolean enabled = true;
        public double size = 0.4;
        public double opacity = 0.45;
        public int color = 0;
        public int leaderColor = 0;
    }

    public static class PointVisual {
        public double markerSize = 14.0;
        public double borderWidth = 0.3;
        public double progressEdgeWidth = 1.0;
        public double riseHeight = 6.0;
        public double extraScale = 0.06;
        public double proximityFade = 3.0;
        public double bobAmplitude = 5.0;
        public double bobPeriod = 3.0;
        public boolean showDistance = true;
        public double zoneRadius = 38.0;
    }

    public static class BaseVisual {
        public double width = 40.0;
        public double height = 14.0;
        public double borderWidth = 1.2;
        public double riseHeight = 6.0;
        public double extraScale = 0.06;
        public double proximityFade = 0.0;
        public double bobAmplitude = 5.0;
        public double bobPeriod = 3.0;
        public boolean showDistance = true;
    }

    public static class BorderVisual {
        public double maxDistance = 120.0;
        public double beamSpacing = 3.0;
        public double beamWidth = 0.05;
        public double beamHeight = 5.0;
        public double fadeDistance = 30.0;
        public double beamRed = 1.0;
        public double beamGreen = 0.3;
        public double beamBlue = 0.1;
    }

    public static class RingVisual {
        public double outerRadius = 38.0;
        public double innerRadius = 26.0;
        public double outerThickness = 1.0;
        public double innerThickness = 0.6;
        public double ringDotsRadius = 38.0;
        public int ringDotsCount = 6;
        public double maxRingDist = 80.0;
        public double maxBeamDist = 80.0;
    }

    public static class BaseRingVisual {
        public double outerRingStroke = 3;
        public double innerRingStroke = 1;
        public double outerThickness = 1.2;
        public double innerThickness = 0.8;
        public double innerRingScale = 0.7;
        public double beamCount = 10;
        public double beamHeight = 8.0;
        public double maxDist = 240.0;
        public double radiusMultiplier = 10.0;
    }

    public static VisualsConfig get() {
        if (instance == null) {
            instance = load();
        }
        ensureDefaults(instance);
        return instance;
    }

    private static void ensureDefaults(VisualsConfig cfg) {
        if (cfg.squadMarker == null) cfg.squadMarker = new SquadMarkerVisual();
    }

    public static void reload() {
        instance = load();
    }

    private static VisualsConfig load() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                VisualsConfig cfg = GSON.fromJson(reader, VisualsConfig.class);
                if (cfg != null) {
                    PWP.LOGGER.info("Loaded visuals config");
                    return cfg;
                }
            } catch (Exception e) {
                PWP.LOGGER.warn("Failed to load visuals config: {}", e.getMessage());
            }
        }
        VisualsConfig def = new VisualsConfig();
        save(def);
        return def;
    }

    public static void save() {
        if (instance != null) {
            save(instance);
        }
    }

    private static void save(VisualsConfig cfg) {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            try (var writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(cfg, writer);
            }
            PWP.LOGGER.info("Saved default visuals config");
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to save visuals config: {}", e.getMessage());
        }
    }

    private static Path getConfigPath() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameDirectory != null) {
            return mc.gameDirectory.toPath().resolve(CONFIG_FILE);
        }
        return Path.of(CONFIG_FILE);
    }
}
