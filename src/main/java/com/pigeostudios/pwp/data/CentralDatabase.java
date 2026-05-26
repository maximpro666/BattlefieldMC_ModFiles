package com.pigeostudios.pwp.data;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.PlayerCombatData;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class CentralDatabase {
    private static Path dbPath;
    private static Connection connection;
    private static boolean initialized = false;

    private static Path resolveDbPath() {
        String override = System.getProperty("pwp.db.path");
        if (override != null) return Path.of(override).toAbsolutePath().normalize();

        // Try to resolve relative to launcher directory (shared between lobby and match server)
        Path[] candidates = {
            Path.of(System.getProperty("user.dir")).resolve("../launcher/database/pwp.db"),
            Path.of(System.getProperty("user.dir")).resolve("launcher/database/pwp.db"),
            Path.of("").toAbsolutePath().resolve("../launcher/database/pwp.db"),
        };
        for (Path p : candidates) {
            Path abs = p.toAbsolutePath().normalize();
            if (abs.getParent() != null) {
                try {
                    Files.createDirectories(abs.getParent());
                    return abs;
                } catch (Exception ignored) {}
            }
        }
        // Absolute fallback inside the mod files project
        return Path.of("").toAbsolutePath().resolve("launcher/database/pwp.db").normalize();
    }

    public static synchronized void init() {
        if (initialized) return;
        try {
            dbPath = resolveDbPath();
            Files.createDirectories(dbPath.getParent());

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA busy_timeout=5000");
            }
            createTables();
            initialized = true;
            PWP.LOGGER.info("CentralDatabase initialized at {}", dbPath);
        } catch (Exception e) {
            PWP.LOGGER.error("Failed to initialize CentralDatabase", e);
        }
    }

    public static synchronized Connection getConnection() {
        if (!initialized) init();
        return connection;
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid TEXT PRIMARY KEY,
                    team_ordinal INT DEFAULT 0,
                    kills INT DEFAULT 0,
                    deaths INT DEFAULT 0,
                    wins INT DEFAULT 0,
                    rating INT DEFAULT 1000,
                    squad_id INT DEFAULT -1,
                    rank_ordinal INT DEFAULT 0,
                    prefix TEXT DEFAULT '',
                    suffix TEXT DEFAULT '',
                    display_name TEXT DEFAULT '',
                    battle_credits INT DEFAULT 0,
                    war_credits INT DEFAULT 0,
                    callsign TEXT DEFAULT '',
                    is_admin INT DEFAULT 0,
                    donat_tier INT DEFAULT 0,
                    player_title TEXT DEFAULT '',
                    loadout_config TEXT DEFAULT '',
                    selected_kit TEXT DEFAULT '',
                    selected_role TEXT DEFAULT '',
                    selected_loadout TEXT DEFAULT '',
                    has_received_dog_tag INT DEFAULT 0,
                    has_chosen_team INT DEFAULT 0,
                    has_accepted_tos INT DEFAULT 0
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS unlocked_kits (
                    uuid TEXT NOT NULL,
                    kit_name TEXT NOT NULL,
                    PRIMARY KEY (uuid, kit_name)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS unlocked_roles (
                    uuid TEXT NOT NULL,
                    role_id TEXT NOT NULL,
                    PRIMARY KEY (uuid, role_id)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS unlocked_loadouts (
                    uuid TEXT NOT NULL,
                    loadout_id TEXT NOT NULL,
                    PRIMARY KEY (uuid, loadout_id)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS certifications (
                    uuid TEXT NOT NULL,
                    cert_id TEXT NOT NULL,
                    PRIMARY KEY (uuid, cert_id)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS saved_attachments (
                    uuid TEXT NOT NULL,
                    attachment_key TEXT NOT NULL,
                    attachment_value TEXT NOT NULL,
                    PRIMARY KEY (uuid, attachment_key)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS match_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    map_name TEXT NOT NULL,
                    winning_team TEXT DEFAULT '',
                    nato_score INT DEFAULT 0,
                    russia_score INT DEFAULT 0,
                    played_at INTEGER NOT NULL
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ticket_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ticket_id INTEGER NOT NULL,
                    sender_uuid TEXT NOT NULL,
                    sender_name TEXT NOT NULL,
                    message TEXT NOT NULL,
                    sent_at INTEGER NOT NULL,
                    is_system INT DEFAULT 0
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vehicle_cooldowns (
                    cooldown_key TEXT PRIMARY KEY,
                    expiry_ms INTEGER NOT NULL
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS match_state (
                    state_key TEXT PRIMARY KEY,
                    state_value TEXT NOT NULL
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bonus_claims (
                    uuid TEXT NOT NULL,
                    bonus_type TEXT NOT NULL,
                    claim_date TEXT NOT NULL,
                    PRIMARY KEY (uuid, bonus_type)
                )
            """);

            // Migration: add columns that may be missing if the table was created by an older version
            try {
                stmt.execute("ALTER TABLE player_data ADD COLUMN has_accepted_tos INT DEFAULT 0");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE player_data ADD COLUMN has_chosen_team INT DEFAULT 0");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE player_data ADD COLUMN has_received_dog_tag INT DEFAULT 0");
            } catch (SQLException ignored) {}
        }
    }

    public static synchronized boolean isInitialized() {
        return initialized;
    }

    public static synchronized void savePlayer(UUID uuid, PlayerCombatData data) {
        if (!initialized) init();
        try {
            String sql = """
                INSERT OR REPLACE INTO player_data
                (uuid, team_ordinal, kills, deaths, wins, rating, squad_id, rank_ordinal,
                 prefix, suffix, display_name, battle_credits, war_credits,
                 callsign, is_admin, donat_tier, player_title,
                 loadout_config, selected_kit, selected_role, selected_loadout,
                 has_received_dog_tag, has_chosen_team, has_accepted_tos)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, data.getTeam().ordinal());
                ps.setInt(3, data.getKills());
                ps.setInt(4, data.getDeaths());
                ps.setInt(5, data.getWins());
                ps.setInt(6, data.getRating());
                ps.setInt(7, data.getSquadId());
                ps.setInt(8, data.getRankOrdinal());
                ps.setString(9, data.getPrefix());
                ps.setString(10, data.getSuffix());
                ps.setString(11, data.getDisplayName());
                ps.setInt(12, data.getBattleCredits());
                ps.setInt(13, data.getWarCredits());
                ps.setString(14, data.getCallsign());
                ps.setInt(15, data.isAdmin() ? 1 : 0);
                ps.setInt(16, data.getDonatTier());
                ps.setString(17, data.getPlayerTitle());
                ps.setString(18, data.getLoadoutConfig());
                ps.setString(19, data.getSelectedKit());
                ps.setString(20, data.getSelectedRole());
                ps.setString(21, data.getSelectedLoadout());
                ps.setInt(22, data.hasReceivedDogTag() ? 1 : 0);
                ps.setInt(23, data.hasChosenTeam() ? 1 : 0);
                ps.setInt(24, data.hasAcceptedTOS() ? 1 : 0);
                ps.executeUpdate();
            }

            saveStringSet(uuid, "unlocked_kits", "kit_name", data.getUnlockedKits());
            saveStringSet(uuid, "unlocked_roles", "role_id", data.getUnlockedRoles());
            saveStringSet(uuid, "unlocked_loadouts", "loadout_id", data.getUnlockedLoadouts());
            saveStringSet(uuid, "certifications", "cert_id", data.getCertifications());
            saveAttachments(uuid, data.getSavedAttachments());

        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to save player {}", uuid, e);
        }
    }

    private static void saveStringSet(UUID uuid, String table, String valueColumn, Set<String> values) throws SQLException {
        try (PreparedStatement del = connection.prepareStatement("DELETE FROM " + table + " WHERE uuid=?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        if (values.isEmpty()) return;
        String sql = "INSERT INTO " + table + " (uuid, " + valueColumn + ") VALUES (?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String v : values) {
                ps.setString(1, uuid.toString());
                ps.setString(2, v);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void saveAttachments(UUID uuid, Map<String, CompoundTag> attachments) throws SQLException {
        try (PreparedStatement del = connection.prepareStatement("DELETE FROM saved_attachments WHERE uuid=?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        if (attachments.isEmpty()) return;
        String sql = "INSERT INTO saved_attachments (uuid, attachment_key, attachment_value) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<String, CompoundTag> e : attachments.entrySet()) {
                ps.setString(1, uuid.toString());
                ps.setString(2, e.getKey());
                ps.setString(3, e.getValue().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static synchronized PlayerCombatData loadPlayer(UUID uuid) {
        if (!initialized) init();
        try {
            String sql = "SELECT * FROM player_data WHERE uuid=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return resultSetToPlayerData(uuid, rs);
                    }
                }
            }
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to load player {}", uuid, e);
        }
        return null;
    }

    public static synchronized Map<UUID, PlayerCombatData> loadAllPlayers() {
        if (!initialized) init();
        Map<UUID, PlayerCombatData> result = new HashMap<>();
        try {
            String sql = "SELECT * FROM player_data";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    result.put(uuid, resultSetToPlayerData(uuid, rs));
                }
            }
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to load all players", e);
        }
        return result;
    }

    public static synchronized void saveAllPlayers(Map<UUID, PlayerCombatData> players) {
        try {
            connection.setAutoCommit(false);
            for (Map.Entry<UUID, PlayerCombatData> e : players.entrySet()) {
                savePlayer(e.getKey(), e.getValue());
            }
            connection.commit();
        } catch (Exception e) {
            try { connection.rollback(); } catch (Exception ignored) {}
            PWP.LOGGER.error("CentralDatabase: failed to save all players, rolled back", e);
        } finally {
            try { connection.setAutoCommit(true); } catch (Exception ignored) {}
        }
    }

    public static synchronized boolean hasPlayer(UUID uuid) {
        if (!initialized) init();
        try {
            String sql = "SELECT 1 FROM player_data WHERE uuid=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static PlayerCombatData resultSetToPlayerData(UUID uuid, ResultSet rs) throws Exception {
        PlayerCombatData data = new PlayerCombatData();
        data.setTeam(Team.fromOrdinal(rs.getInt("team_ordinal")));
        data.setKills(rs.getInt("kills"));
        data.setDeaths(rs.getInt("deaths"));
        data.setWins(rs.getInt("wins"));
        data.setRating(rs.getInt("rating"));
        data.setSquadId(rs.getInt("squad_id"));
        data.setRankOrdinal(rs.getInt("rank_ordinal"));
        data.setPrefix(rs.getString("prefix"));
        data.setSuffix(rs.getString("suffix"));
        data.setDisplayName(rs.getString("display_name"));
        data.setBattleCredits(rs.getInt("battle_credits"));
        data.setWarCredits(rs.getInt("war_credits"));
        data.setCallsign(rs.getString("callsign"));
        data.setAdmin(rs.getInt("is_admin") == 1);
        data.setDonatTier(rs.getInt("donat_tier"));
        data.setPlayerTitle(rs.getString("player_title"));
        data.setLoadoutConfig(rs.getString("loadout_config"));
        data.setSelectedKit(rs.getString("selected_kit"));
        data.setSelectedRole(rs.getString("selected_role"));
        data.setSelectedLoadout(rs.getString("selected_loadout"));
        data.setHasReceivedDogTag(rs.getInt("has_received_dog_tag") == 1);
        data.setHasChosenTeam(rs.getInt("has_chosen_team") == 1);
        data.setHasAcceptedTOS(rs.getInt("has_accepted_tos") == 1);

        loadStringSet(uuid, "unlocked_kits", "kit_name", data.getUnlockedKits());
        loadStringSet(uuid, "unlocked_roles", "role_id", data.getUnlockedRoles());
        loadStringSet(uuid, "unlocked_loadouts", "loadout_id", data.getUnlockedLoadouts());
        loadStringSet(uuid, "certifications", "cert_id", data.getCertifications());
        loadAttachments(uuid, data.getSavedAttachments());

        return data;
    }

    private static void loadStringSet(UUID uuid, String table, String valueColumn, Set<String> target) throws SQLException {
        target.clear();
        String sql = "SELECT " + valueColumn + " FROM " + table + " WHERE uuid=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    target.add(rs.getString(1));
                }
            }
        }
    }

    private static void loadAttachments(UUID uuid, Map<String, CompoundTag> target) throws Exception {
        target.clear();
        String sql = "SELECT attachment_key, attachment_value FROM saved_attachments WHERE uuid=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("attachment_key");
                    String val = rs.getString("attachment_value");
                    try {
                        target.put(key, TagParser.parseTag(val));
                    } catch (Exception e) {
                        PWP.LOGGER.warn("CentralDatabase: failed to parse attachment {} for {}", key, uuid);
                    }
                }
            }
        }
    }

    public static synchronized void recordMatchPlayed(String mapName, String winningTeam, int natoScore, int russiaScore) {
        if (!initialized) init();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO match_history (map_name, winning_team, nato_score, russia_score, played_at) VALUES (?,?,?,?,?)")) {
            ps.setString(1, mapName);
            ps.setString(2, winningTeam != null ? winningTeam : "");
            ps.setInt(3, natoScore);
            ps.setInt(4, russiaScore);
            ps.setLong(5, System.currentTimeMillis() / 1000);
            ps.executeUpdate();
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to record match", e);
        }
    }

    public static synchronized List<String> getRecentPlayedMaps(int limit) {
        if (!initialized) init();
        List<String> recent = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DISTINCT map_name FROM match_history ORDER BY played_at DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    recent.add(rs.getString("map_name"));
                }
            }
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to load recent maps", e);
        }
        return recent;
    }

    public static synchronized String getLastPlayedMap() {
        if (!initialized) init();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT map_name FROM match_history ORDER BY played_at DESC LIMIT 1")) {
            if (rs.next()) return rs.getString("map_name");
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to get last map", e);
        }
        return null;
    }

    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                PWP.LOGGER.warn("CentralDatabase: error closing connection", e);
            }
            connection = null;
            initialized = false;
        }
    }

    // ===== Vehicle Cooldowns =====

    public static synchronized void saveVehicleCooldowns(Map<String, Long> cooldowns) {
        if (!initialized) init();
        try (PreparedStatement del = connection.prepareStatement("DELETE FROM vehicle_cooldowns")) {
            del.executeUpdate();
        } catch (Exception e) {
            PWP.LOGGER.warn("CentralDatabase: failed to clear vehicle_cooldowns", e);
            return;
        }
        if (cooldowns.isEmpty()) return;
        String sql = "INSERT OR REPLACE INTO vehicle_cooldowns (cooldown_key, expiry_ms) VALUES (?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> e : cooldowns.entrySet()) {
                if (e.getValue() > now) {
                    ps.setString(1, e.getKey());
                    ps.setLong(2, e.getValue());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to save vehicle cooldowns", e);
        }
    }

    public static synchronized Map<String, Long> loadVehicleCooldowns() {
        if (!initialized) init();
        Map<String, Long> result = new HashMap<>();
        long now = System.currentTimeMillis();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT cooldown_key, expiry_ms FROM vehicle_cooldowns")) {
            while (rs.next()) {
                long expiry = rs.getLong("expiry_ms");
                if (expiry > now) {
                    result.put(rs.getString("cooldown_key"), expiry);
                }
            }
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to load vehicle cooldowns", e);
        }
        return result;
    }

    // ===== Match State (map selection, etc.) =====

    public static synchronized void setMatchState(String key, String value) {
        if (!initialized) init();
        String sql = "INSERT OR REPLACE INTO match_state (state_key, state_value) VALUES (?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to set match state '{}'", key, e);
        }
    }

    public static synchronized String getMatchState(String key) {
        if (!initialized) init();
        String sql = "SELECT state_value FROM match_state WHERE state_key=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("state_value");
            }
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to get match state '{}'", key, e);
        }
        return null;
    }

    public static synchronized void removeMatchState(String key) {
        if (!initialized) init();
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM match_state WHERE state_key=?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to remove match state '{}'", key, e);
        }
    }

    public static synchronized void setSelectedMap(String mapName) {
        setMatchState("selected_map", mapName);
    }

    public static synchronized String getSelectedMap() {
        return getMatchState("selected_map");
    }

    public static synchronized void clearSelectedMap() {
        removeMatchState("selected_map");
    }

    // ===== Bonus Claims (daily, weekly) =====

    public static synchronized void saveBonusClaim(UUID uuid, String bonusType, String claimDate) {
        if (!initialized) init();
        String sql = "INSERT OR REPLACE INTO bonus_claims (uuid, bonus_type, claim_date) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, bonusType);
            ps.setString(3, claimDate);
            ps.executeUpdate();
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to save bonus claim {} for {}", bonusType, uuid, e);
        }
    }

    public static synchronized String getBonusClaim(UUID uuid, String bonusType) {
        if (!initialized) init();
        String sql = "SELECT claim_date FROM bonus_claims WHERE uuid=? AND bonus_type=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, bonusType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("claim_date");
            }
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to get bonus claim {} for {}", bonusType, uuid, e);
        }
        return null;
    }

    public static synchronized Map<UUID, String> loadAllBonusClaims(String bonusType) {
        if (!initialized) init();
        Map<UUID, String> result = new HashMap<>();
        String sql = "SELECT uuid, claim_date FROM bonus_claims WHERE bonus_type=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bonusType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        result.put(UUID.fromString(rs.getString("uuid")), rs.getString("claim_date"));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: failed to load bonus claims for type {}", bonusType, e);
        }
        return result;
    }

    // ===== Functional helpers for punishment/report managers =====

    @FunctionalInterface
    public interface SqlExecutor { void run(Connection conn) throws SQLException; }

    @FunctionalInterface
    public interface SqlQuery<T> { T run(Connection conn) throws SQLException; }

    public static synchronized void execute(SqlExecutor task) {
        if (!initialized) init();
        try {
            task.run(connection);
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: execute error", e);
        }
    }

    public static synchronized <T> T query(SqlQuery<T> task) {
        if (!initialized) init();
        try {
            return task.run(connection);
        } catch (Exception e) {
            PWP.LOGGER.error("CentralDatabase: query error", e);
            return null;
        }
    }
}
