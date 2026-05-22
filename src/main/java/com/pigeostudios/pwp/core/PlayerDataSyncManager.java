package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.data.CentralDatabase;
import com.pigeostudios.pwp.proxy.ProxyMessenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.*;
import java.util.*;

public class PlayerDataSyncManager {

    public static void exportMatchData(MinecraftServer server) {
        if (!ProxyMessenger.isMatchServer()) return;
        try {
            CentralDatabase.init();

            TeamManager tm = PWP.getTeamManager();
            BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();

            Map<UUID, PlayerCombatData> allData = new HashMap<>();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                UUID uuid = p.getUUID();
                PlayerCombatData data = tm.getOrCreatePlayerData(uuid);
                int wc = runtime.getWC(uuid);
                data.setWarCredits(Math.max(0, wc));
                allData.put(uuid, data);
            }

            CentralDatabase.saveAllPlayers(allData);
            exportVehicleCooldowns();
            PWP.LOGGER.info("Exported sync data for {} players to CentralDatabase", allData.size());

        } catch (Exception e) {
            PWP.LOGGER.error("Failed to export match sync data", e);
        }
    }

    public static void importMatchData(MinecraftServer server) {
        try {
            CentralDatabase.init();

            Map<UUID, PlayerCombatData> allData = CentralDatabase.loadAllPlayers();
            if (allData.isEmpty()) {
                PWP.LOGGER.info("No player sync data to import from CentralDatabase");
                return;
            }

            TeamManager tm = PWP.getTeamManager();
            BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
            int count = 0;

            for (Map.Entry<UUID, PlayerCombatData> entry : allData.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerCombatData dbData = entry.getValue();
                PlayerCombatData data = tm.getOrCreatePlayerData(uuid);

                data.setKills(dbData.getKills());
                data.setDeaths(dbData.getDeaths());
                data.setCallsign(dbData.getCallsign());
                data.setDisplayName(dbData.getDisplayName());
                data.setHasReceivedDogTag(dbData.hasReceivedDogTag());

                int wc = dbData.getWarCredits();
                runtime.setWC(uuid, wc);
                data.setWarCredits(wc);

                int bc = dbData.getBattleCredits();
                runtime.setBC(uuid, bc);
                data.setBattleCredits(bc);

                for (String k : dbData.getUnlockedKits()) data.unlockKit(k);
                for (String r : dbData.getUnlockedRoles()) data.unlockRole(r);
                for (String l : dbData.getUnlockedLoadouts()) data.unlockLoadout(l);
                for (String c : dbData.getCertifications()) data.grantCertification(c);

                for (Map.Entry<String, net.minecraft.nbt.CompoundTag> att : dbData.getSavedAttachments().entrySet()) {
                    data.getSavedAttachments().put(att.getKey(), att.getValue());
                }

                count++;
            }

            tm.setDirty();
            importVehicleCooldowns();
            PWP.LOGGER.info("Imported sync data for {} players from CentralDatabase", count);

        } catch (Exception e) {
            PWP.LOGGER.error("Failed to import match sync data", e);
        }
    }

    private static void exportVehicleCooldowns() {
        try {
            CentralDatabase.init();
            Path src = Paths.get("config/pwp/vehicle_cooldowns.json");
            if (Files.exists(src)) {
                Path dest = Path.of("../launcher/database/vehicle_cooldowns.json");
                Files.createDirectories(dest.getParent());
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                PWP.LOGGER.info("Exported vehicle cooldowns");
            }
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to export vehicle cooldowns: {}", e.getMessage());
        }
    }

    private static void importVehicleCooldowns() {
        try {
            Path src = Path.of("../launcher/database/vehicle_cooldowns.json");
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
    }
}
