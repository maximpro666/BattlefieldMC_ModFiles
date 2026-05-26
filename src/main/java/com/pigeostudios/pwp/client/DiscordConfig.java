package com.pigeostudios.pwp.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pigeostudios.pwp.PWP;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiscordConfig {
    private static final Path CONFIG_PATH = Path.of("config", "pwp", "discord.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;
    private String applicationId = "1508368614477271072";

    public boolean isEnabled() { return enabled; }
    public String getApplicationId() { return applicationId; }

    public static DiscordConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                DiscordConfig cfg = GSON.fromJson(Files.readString(CONFIG_PATH), DiscordConfig.class);
                if (cfg != null && cfg.applicationId != null && !cfg.applicationId.isEmpty()) {
                    return cfg;
                }
            } catch (IOException e) {
                PWP.LOGGER.error("Failed to load Discord config: {}", e.getMessage());
            }
        }
        DiscordConfig defaults = new DiscordConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to save Discord config: {}", e.getMessage());
        }
    }
}
