package com.pigeostudios.pwp.core;

import com.google.gson.*;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.proxy.ProxyMessenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.*;
import java.util.*;

public class PlayerDataSyncManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getLauncherDir() {
        return Path.of(System.getProperty("user.dir")).resolve("../launcher/sync_data").normalize();
    }

    public static void exportMatchData(MinecraftServer server) {
        if (!ProxyMessenger.isMatchServer()) return;
        try {
            Path syncDir = getLauncherDir();
            Files.createDirectories(syncDir);

            TeamManager tm = PWP.getTeamManager();
            BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();

            JsonObject root = new JsonObject();
            root.addProperty("matchId", System.currentTimeMillis());
            root.addProperty("serverPort", server.getPort());

            JsonObject players = new JsonObject();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                UUID uuid = p.getUUID();
                PlayerCombatData data = tm.getOrCreatePlayerData(uuid);

                JsonObject obj = new JsonObject();
                obj.addProperty("kills", data.getKills());
                obj.addProperty("deaths", data.getDeaths());

                int wc = runtime.getWC(uuid);
                obj.addProperty("warCredits", Math.max(0, wc));

                JsonArray kits = new JsonArray();
                for (String k : data.getUnlockedKits()) kits.add(k);
                obj.add("unlockedKits", kits);

                JsonArray roles = new JsonArray();
                for (String r : data.getUnlockedRoles()) roles.add(r);
                obj.add("unlockedRoles", roles);

                JsonArray loadouts = new JsonArray();
                for (String l : data.getUnlockedLoadouts()) loadouts.add(l);
                obj.add("unlockedLoadouts", loadouts);

                JsonArray certs = new JsonArray();
                for (String c : data.getCertifications()) certs.add(c);
                obj.add("certifications", certs);

                JsonArray attachments = new JsonArray();
                for (Map.Entry<String, net.minecraft.nbt.CompoundTag> e : data.getSavedAttachments().entrySet()) {
                    JsonObject att = new JsonObject();
                    att.addProperty("key", e.getKey());
                    att.addProperty("value", e.getValue().toString());
                    attachments.add(att);
                }
                obj.add("savedAttachments", attachments);

                players.add(uuid.toString(), obj);
            }
            root.add("players", players);

            Path file = syncDir.resolve("match_sync.json");
            Files.writeString(file, GSON.toJson(root));
            PWP.LOGGER.info("Exported sync data for {} players", players.size());

            // Also export vehicle cooldowns
            exportVehicleCooldowns(syncDir);

        } catch (Exception e) {
            PWP.LOGGER.error("Failed to export match sync data", e);
        }
    }

    public static void importMatchData(MinecraftServer server) {
        try {
            Path syncDir = getLauncherDir();
            Path file = syncDir.resolve("match_sync.json");
            if (!Files.exists(file)) return;

            String content = Files.readString(file);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            JsonObject players = root.getAsJsonObject("players");

            if (players == null || players.size() == 0) {
                PWP.LOGGER.info("No player sync data to import");
                Files.delete(file);
                return;
            }

            TeamManager tm = PWP.getTeamManager();
            BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
            int count = 0;

            for (String uuidStr : players.keySet()) {
                UUID uuid = UUID.fromString(uuidStr);
                JsonObject obj = players.getAsJsonObject(uuidStr);
                PlayerCombatData data = tm.getOrCreatePlayerData(uuid);

                if (obj.has("kills")) data.setKills(obj.get("kills").getAsInt());
                if (obj.has("deaths")) data.setDeaths(obj.get("deaths").getAsInt());

                if (obj.has("warCredits")) {
                    int wc = obj.get("warCredits").getAsInt();
                    runtime.setWC(uuid, wc);
                    data.setWarCredits(wc);
                }

                if (obj.has("unlockedKits")) {
                    for (JsonElement e : obj.getAsJsonArray("unlockedKits"))
                        data.unlockKit(e.getAsString());
                }
                if (obj.has("unlockedRoles")) {
                    for (JsonElement e : obj.getAsJsonArray("unlockedRoles"))
                        data.unlockRole(e.getAsString());
                }
                if (obj.has("unlockedLoadouts")) {
                    for (JsonElement e : obj.getAsJsonArray("unlockedLoadouts"))
                        data.unlockLoadout(e.getAsString());
                }
                if (obj.has("certifications")) {
                    for (JsonElement e : obj.getAsJsonArray("certifications"))
                        data.grantCertification(e.getAsString());
                }
                if (obj.has("savedAttachments")) {
                    for (JsonElement e : obj.getAsJsonArray("savedAttachments")) {
                        JsonObject att = e.getAsJsonObject();
                        String key = att.get("key").getAsString();
                        String val = att.get("value").getAsString();
                        try {
                            data.getSavedAttachments().put(key, net.minecraft.nbt.TagParser.parseTag(val));
                        } catch (Exception ex) {
                            PWP.LOGGER.warn("Failed to parse attachment for {}: {}", uuid, ex.getMessage());
                        }
                    }
                }

                count++;
            }

            tm.setDirty();

            // Import vehicle cooldowns
            importVehicleCooldowns(syncDir);

            // Clean up
            Files.delete(file);
            PWP.LOGGER.info("Imported sync data for {} players", count);

        } catch (Exception e) {
            PWP.LOGGER.error("Failed to import match sync data", e);
        }
    }

    private static void exportVehicleCooldowns(Path syncDir) {
        try {
            Path src = Paths.get("config/pwp/vehicle_cooldowns.json");
            if (Files.exists(src)) {
                Files.copy(src, syncDir.resolve("vehicle_cooldowns.json"), StandardCopyOption.REPLACE_EXISTING);
                PWP.LOGGER.info("Exported vehicle cooldowns");
            }
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to export vehicle cooldowns: {}", e.getMessage());
        }
    }

    public static void importVehicleCooldowns(Path syncDir) {
        try {
            Path src = syncDir.resolve("vehicle_cooldowns.json");
            if (!Files.exists(src)) return;
            Path dest = Paths.get("config/pwp/vehicle_cooldowns.json");
            Files.createDirectories(dest.getParent());
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            PWP.LOGGER.info("Imported vehicle cooldowns");
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to import vehicle cooldowns: {}", e.getMessage());
        }
    }

    public static void cleanupSyncDir() {
        try {
            Path syncDir = getLauncherDir();
            if (Files.exists(syncDir)) {
                Path file = syncDir.resolve("match_sync.json");
                Files.deleteIfExists(file);
                PWP.LOGGER.info("Cleaned up stale sync data");
            }
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to clean sync dir: {}", e.getMessage());
        }
    }
}
