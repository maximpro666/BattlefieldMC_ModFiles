package com.pigeostudios.pwp.service;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.PlayerCombatData;
import com.pigeostudios.pwp.data.CentralDatabase;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

public class PersistenceService {
    private boolean initialized;

    public void init() {
        CentralDatabase.init();
        initialized = true;
        PWP.LOGGER.info("PersistenceService initialized");
    }

    public boolean isInitialized() { return initialized; }

    // ════════════════════════════════════════
    // BC — single field operations
    // ════════════════════════════════════════

    public void saveBC(UUID uuid, int amount) {
        executeUpdate("INSERT INTO player_data (uuid, battle_credits) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET battle_credits = ?",
            ps -> { try { ps.setString(1, uuid.toString()); ps.setInt(2, amount); ps.setInt(3, amount); } catch (SQLException e) {} });
    }

    public int loadBC(UUID uuid) {
        return queryInt("SELECT battle_credits FROM player_data WHERE uuid = ?",
            uuid.toString(), 0);
    }

    // ════════════════════════════════════════
    // WC — single field operations
    // ════════════════════════════════════════

    public void saveWC(UUID uuid, int amount) {
        executeUpdate("INSERT INTO player_data (uuid, war_credits) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET war_credits = ?",
            ps -> { try { ps.setString(1, uuid.toString()); ps.setInt(2, amount); ps.setInt(3, amount); } catch (SQLException e) {} });
    }

    public int loadWC(UUID uuid) {
        return queryInt("SELECT war_credits FROM player_data WHERE uuid = ?",
            uuid.toString(), 0);
    }

    // ════════════════════════════════════════
    // Full player data
    // ════════════════════════════════════════

    public void saveFullPlayerData(UUID uuid, PlayerCombatData data) {
        CentralDatabase.savePlayer(uuid, data);
    }

    public PlayerCombatData loadFullPlayerData(UUID uuid) {
        return CentralDatabase.loadPlayer(uuid);
    }

    public Map<UUID, PlayerCombatData> loadAllPlayers() {
        return CentralDatabase.loadAllPlayers();
    }

    public void saveAllPlayers(Map<UUID, PlayerCombatData> players) {
        CentralDatabase.saveAllPlayers(players);
    }

    // ════════════════════════════════════════
    // Match history
    // ════════════════════════════════════════

    public void recordMatchPlayed(String mapName, String winningTeam, int natoScore, int russiaScore) {
        CentralDatabase.recordMatchPlayed(mapName, winningTeam, natoScore, russiaScore);
    }

    public List<String> getRecentPlayedMaps(int limit) {
        return CentralDatabase.getRecentPlayedMaps(limit);
    }

    // ════════════════════════════════════════
    // Shutdown
    // ════════════════════════════════════════

    public void shutdown() {
        CentralDatabase.close();
        initialized = false;
        PWP.LOGGER.info("PersistenceService shut down");
    }

    // ════════════════════════════════════════
    // Internal helpers
    // ════════════════════════════════════════

    private void executeUpdate(String sql, Consumer<PreparedStatement> binder) {
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                binder.accept(ps);
                ps.executeUpdate();
            }
        });
    }

    private int queryInt(String sql, String uuidStr, int defaultVal) {
        Integer result = CentralDatabase.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuidStr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            return defaultVal;
        });
        return result != null ? result : defaultVal;
    }
}
