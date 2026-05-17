package com.yourmod.teamsystem.core;

import com.google.gson.*;
import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class KitManager {
    private static final String CONFIG_DIR = "config/teamsystem";
    private static final String KITS_FILE = CONFIG_DIR + "/kits.json";

    private Map<String, Kit> kits = new HashMap<>();
    private Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public KitManager() {
        loadKits();
    }

    public void loadKits() {
        try {
            Path configPath = Paths.get(KITS_FILE);
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                JsonArray arr = JsonParser.parseString(content).getAsJsonArray();
                for (JsonElement elem : arr) {
                    Kit kit = Kit.fromJson(elem.getAsJsonObject());
                    kits.put(kit.getName(), kit);
                }
                TeamSystem.LOGGER.info("Loaded {} kits", kits.size());
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to load kits: {}", e.getMessage());
        }
    }

    public void saveKits() {
        try {
            Path configPath = Paths.get(CONFIG_DIR);
            Files.createDirectories(configPath);

            JsonArray arr = new JsonArray();
            for (Kit kit : kits.values()) {
                arr.add(kit.toJson());
            }

            Files.writeString(Paths.get(KITS_FILE), new GsonBuilder().setPrettyPrinting().create().toJson(arr));
            TeamSystem.LOGGER.info("Saved {} kits", kits.size());
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to save kits: {}", e.getMessage());
        }
    }

    public void createKit(String name, String displayName, Team team, int minRankOrdinal, List<ItemStack> items) {
        Kit kit = new Kit(name, displayName, team, minRankOrdinal, 0, items);
        kits.put(name, kit);
        saveKits();
        TeamSystem.LOGGER.info("Created kit: {}", name);
    }

    public void deleteKit(String name) {
        if (kits.remove(name) != null) {
            saveKits();
            TeamSystem.LOGGER.info("Deleted kit: {}", name);
        }
    }

    public Kit getKit(String name) {
        return kits.get(name);
    }

    public List<Kit> getAvailableKits(ServerPlayer player, TeamManager teamManager) {
        List<Kit> available = new ArrayList<>();
        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        int playerRank = teamManager.getOrCreatePlayerData(player.getUUID()).getRankOrdinal();

        for (Kit kit : kits.values()) {
            if ((kit.getTeam() == Team.SPECTATOR || kit.getTeam() == playerTeam) &&
                playerRank >= kit.getMinRankOrdinal()) {
                available.add(kit);
            }
        }
        return available;
    }

    public boolean claimKit(ServerPlayer player, String kitName, TeamManager teamManager) {
        Kit kit = getKit(kitName);
        if (kit == null) return false;

        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        int playerRank = teamManager.getOrCreatePlayerData(player.getUUID()).getRankOrdinal();

        if (kit.getTeam() != Team.SPECTATOR && kit.getTeam() != playerTeam) {
            return false;
        }

        if (playerRank < kit.getMinRankOrdinal()) {
            return false;
        }

        if (kit.getCooldownSeconds() > 0) {
            if (isOnCooldown(player.getUUID(), kitName)) {
                return false;
            }
            setCooldown(player.getUUID(), kitName, kit.getCooldownSeconds());
        }

        if (!player.isAlive()) {
            return false;
        }

        player.getInventory().clearContent();
        for (ItemStack item : kit.getItems()) {
            if (!item.isEmpty()) {
                player.addItem(item.copy());
            }
        }

        return true;
    }

    private boolean isOnCooldown(UUID playerId, String kitName) {
        Map<String, Long> playerCooldowns = cooldowns.getOrDefault(playerId, new HashMap<>());
        Long cooldownEnd = playerCooldowns.get(kitName);
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    private void setCooldown(UUID playerId, String kitName, int cooldownSeconds) {
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        playerCooldowns.put(kitName, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    public Map<String, Kit> getKits() {
        return kits;
    }
}
