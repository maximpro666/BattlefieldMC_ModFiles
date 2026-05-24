package com.pigeostudios.pwp.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pigeostudios.pwp.PWP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PWPConfig {
    private static final String FILE_NAME = "pwp_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String language = "ru";
    private int minPlayersToStart = 2;
    private int maxPlayers = 64;
    private int lobbyWaitTime = 30;
    private int votingTime = 30;
    private int countdownSeconds = 15;
    private int killRewardBC = 5;
    private int vehicleKillRewardBC = 10;
    private int winRewardBC = 25;
    private int winRewardWC = 50;
    private int captureRewardBC = 150;
    private int spawnProtectionTicks = 100;
    private int maxBeaconsPerPlayer = 3;
    private int respawnDelay = 5;
    private int minDistanceFromEnemyBase = 50;
    private int minDistanceFromCapturePoint = 30;
    private int minDistanceFromOwnBase = 10;
    private int ticketsPerMap = 100;
    private int regenCooldownSeconds = 300;
    private int baseRadius = 30;
    private int worldBorderSize = 1000;
    private boolean restartAfterMaintenance = true;
    private boolean autoStartVoting = true;
    private boolean teamBalancing = true;
    private int maxTeamDifference = 1;
    private int maxFOBsPerTeam = 3;
    private int fobCostSP = 100;
    private boolean beaconsEnabled = false;

    private boolean bleedingEnabled = true;
    private int bleedoutTimeSeconds = 30;
    private int reviveTimeSeconds = 6;
    private float healthAfterRevive = 10.0f;
    private float bleedingHealth = 10.0f;
    private java.util.List<String> bleedingBypassSources = new java.util.ArrayList<>(java.util.Arrays.asList("fellOutOfWorld", "genericKill", "outOfBorder"));

    private Map<String, String> messages = new LinkedHashMap<>();

    public PWPConfig() {
        messages.put("prefix", "&6[PWP]");
        messages.put("voting_started", "&e\u0413\u043e\u043b\u043e\u0441\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430 \u043a\u0430\u0440\u0442\u0443 \u043d\u0430\u0447\u0430\u043b\u043e\u0441\u044c! &6/map vote <name>");
        messages.put("voting_ended", "&e\u041a\u0430\u0440\u0442\u0430 &6{map} &e\u043f\u043e\u0431\u0435\u0434\u0438\u043b\u0430 \u0432 \u0433\u043e\u043b\u043e\u0441\u043e\u0432\u0430\u043d\u0438\u0438!");
        messages.put("game_starting", "&e\u0418\u0433\u0440\u0430 \u043d\u0430\u0447\u0438\u043d\u0430\u0435\u0442\u0441\u044f \u0447\u0435\u0440\u0435\u0437 {sec} \u0441\u0435\u043a\u0443\u043d\u0434...");
        messages.put("game_started", "&a\u0418\u0433\u0440\u0430 \u043d\u0430\u0447\u0430\u043b\u0430\u0441\u044c \u043d\u0430 &6{map}&a!");
        messages.put("game_ended", "&c\u0418\u0433\u0440\u0430 \u043e\u043a\u043e\u043d\u0447\u0435\u043d\u0430!");
        messages.put("team_nato", "&9\u041d\u0410\u0422\u041e");
        messages.put("team_russia", "&c\u0420\u041e\u0421\u0421\u0418\u042f");
        messages.put("team_spectator", "&7\u041d\u0430\u0431\u043b\u044e\u0434\u0430\u0442\u0435\u043b\u044c");
        messages.put("kill_feed_player", "&6{victim} &e\u0443\u0431\u0438\u0442 &6{killer}");
        messages.put("kill_feed_vehicle", "&c\u0422\u0435\u0445\u043d\u0438\u043a\u0430 &6{vehicle} &c\u0443\u043d\u0438\u0447\u0442\u043e\u0436\u0435\u043d\u0430 &6{killer}");
        messages.put("kill_reward", "&a+{bc} BC");
        messages.put("rank_up", "&e\u041f\u043e\u0437\u0434\u0440\u0430\u0432\u043b\u044f\u0435\u043c! \u0412\u044b \u043f\u043e\u043b\u0443\u0447\u0438\u043b\u0438 \u0437\u0432\u0430\u043d\u0438\u0435 &6{rank}");
    }

    public String getLanguage() { return language; }
    public int getMinPlayersToStart() { return minPlayersToStart; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getLobbyWaitTime() { return lobbyWaitTime; }
    public int getVotingTime() { return votingTime; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public int getKillRewardBC() { return killRewardBC; }
    public int getVehicleKillRewardBC() { return vehicleKillRewardBC; }
    public int getWinRewardBC() { return winRewardBC; }
    public int getWinRewardWC() { return winRewardWC; }
    public int getCaptureRewardBC() { return captureRewardBC; }
    public int getSpawnProtectionTicks() { return spawnProtectionTicks; }
    public int getMaxBeaconsPerPlayer() { return maxBeaconsPerPlayer; }
    public int getRespawnDelay() { return respawnDelay; }
    public int getMinDistanceFromEnemyBase() { return minDistanceFromEnemyBase; }
    public int getMinDistanceFromCapturePoint() { return minDistanceFromCapturePoint; }
    public int getMinDistanceFromOwnBase() { return minDistanceFromOwnBase; }
    public int getTicketsPerMap() { return ticketsPerMap; }
    public int getRegenCooldownSeconds() { return regenCooldownSeconds; }
    public int getBaseRadius() { return baseRadius; }
    public int getWorldBorderSize() { return worldBorderSize; }
    public boolean isRestartAfterMaintenance() { return restartAfterMaintenance; }
    public boolean isAutoStartVoting() { return autoStartVoting; }
    public boolean isTeamBalancing() { return teamBalancing; }
    public int getMaxTeamDifference() { return maxTeamDifference; }
    public int getMaxFOBsPerTeam() { return maxFOBsPerTeam; }
    public int getFOBCost() { return fobCostSP; }
    public boolean isBeaconsEnabled() { return beaconsEnabled; }

    public boolean isBleedingEnabled() { return bleedingEnabled; }
    public int getBleedoutTimeSeconds() { return bleedoutTimeSeconds; }
    public int getReviveTimeSeconds() { return reviveTimeSeconds; }
    public float getHealthAfterRevive() { return healthAfterRevive; }
    public float getBleedingHealth() { return bleedingHealth; }
    public List<String> getBleedingBypassSources() { return bleedingBypassSources; }

    public String getMessage(String key) { return translateColors(messages.getOrDefault(key, "&f" + key)); }
    public String getMessage(String key, Map<String, String> placeholders) {
        String msg = messages.getOrDefault(key, "&f" + key);
        for (Map.Entry<String, String> e : placeholders.entrySet())
            msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        return translateColors(msg);
    }

    private static String translateColors(String input) {
        return input.replace('&', '\u00a7');
    }

    public static PWPConfig load(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                PWPConfig config = GSON.fromJson(reader, PWPConfig.class);
                if (config != null) return config;
            } catch (IOException e) {
                PWP.LOGGER.error("Failed to load pwp config: {}", e.getMessage());
            }
        }
        PWPConfig defaults = new PWPConfig();
        defaults.save(server);
        return defaults;
    }

    public void save(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to save pwp config: {}", e.getMessage());
        }
    }
}
