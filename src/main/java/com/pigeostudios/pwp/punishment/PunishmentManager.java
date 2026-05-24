package com.pigeostudios.pwp.punishment;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.data.CentralDatabase;
import com.pigeostudios.pwp.proxy.ProxyMessenger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.sql.*;
import java.util.*;

public class PunishmentManager {
    private static final Map<UUID, ActiveBan> banCache = new HashMap<>();
    private static final Map<UUID, ActiveMute> muteCache = new HashMap<>();
    private static boolean initialized = false;
    private static int nextPunishmentId = 1;

    private static final class ActiveBan {
        final int punishmentId;
        final String reason;
        final long expiresAt;

        ActiveBan(int punishmentId, String reason, long expiresAt) {
            this.punishmentId = punishmentId;
            this.reason = reason;
            this.expiresAt = expiresAt;
        }
    }

    private static final class ActiveMute {
        final int punishmentId;
        final String type;
        final String reason;
        final long expiresAt;

        ActiveMute(int punishmentId, String type, String reason, long expiresAt) {
            this.punishmentId = punishmentId;
            this.type = type;
            this.reason = reason;
            this.expiresAt = expiresAt;
        }
    }

    public static synchronized void init() {
        if (initialized) return;
        CentralDatabase.execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        punisher_uuid TEXT NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT DEFAULT '',
                        reason TEXT DEFAULT '',
                        duration_seconds INTEGER DEFAULT 0,
                        issued_at INTEGER NOT NULL,
                        expires_at INTEGER DEFAULT 0,
                        active INTEGER DEFAULT 1,
                        server_id TEXT DEFAULT ''
                    )
                """);
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS active_bans (
                        uuid TEXT PRIMARY KEY,
                        punishment_id INTEGER NOT NULL,
                        reason TEXT DEFAULT '',
                        ip TEXT DEFAULT '',
                        hwid TEXT DEFAULT '',
                        expires_at INTEGER DEFAULT 0,
                        banned_at INTEGER NOT NULL
                    )
                """);
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS active_mutes (
                        uuid TEXT PRIMARY KEY,
                        punishment_id INTEGER NOT NULL,
                        type TEXT DEFAULT 'CHAT',
                        reason TEXT DEFAULT '',
                        expires_at INTEGER DEFAULT 0
                    )
                """);
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS warn_counts (
                        uuid TEXT NOT NULL,
                        category TEXT NOT NULL,
                        count INTEGER DEFAULT 0,
                        last_warn_at INTEGER DEFAULT 0,
                        PRIMARY KEY (uuid, category)
                    )
                """);
            }
        });
        loadActiveBans();
        loadActiveMutes();
        initialized = true;
        PWP.LOGGER.info("PunishmentManager initialized");
    }

    private static void loadActiveBans() {
        banCache.clear();
        CentralDatabase.execute(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT uuid, punishment_id, reason, expires_at, banned_at FROM active_bans")) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    banCache.put(uuid, new ActiveBan(
                        rs.getInt("punishment_id"),
                        rs.getString("reason"),
                        rs.getLong("expires_at")
                    ));
                }
            }
        });
        banCache.values().removeIf(b -> b.expiresAt > 0 && System.currentTimeMillis() / 1000 > b.expiresAt);
    }

    private static void loadActiveMutes() {
        muteCache.clear();
        CentralDatabase.execute(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT uuid, punishment_id, type, reason, expires_at FROM active_mutes")) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    muteCache.put(uuid, new ActiveMute(
                        rs.getInt("punishment_id"),
                        rs.getString("type"),
                        rs.getString("reason"),
                        rs.getLong("expires_at")
                    ));
                }
            }
        });
        muteCache.values().removeIf(m -> m.expiresAt > 0 && System.currentTimeMillis() / 1000 > m.expiresAt);
    }

    public static int issuePunishment(UUID uuid, UUID punisherUuid, PunishmentType type,
                                       WarnCategory category, String reason, long durationSeconds) {
        long now = System.currentTimeMillis() / 1000;
        long expiresAt = type.isPermanent() ? 0 : now + durationSeconds;
        String serverId = ProxyMessenger.isMatchServer() ? "match" : "lobby";

        int[] generatedId = new int[1];
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO punishments (uuid, punisher_uuid, type, category, reason, duration_seconds, issued_at, expires_at, active, server_id) VALUES (?,?,?,?,?,?,?,?,1,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, punisherUuid.toString());
                ps.setString(3, type.name());
                ps.setString(4, category != null ? category.name() : "");
                ps.setString(5, reason);
                ps.setLong(6, durationSeconds);
                ps.setLong(7, now);
                ps.setLong(8, expiresAt);
                ps.setString(9, serverId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    generatedId[0] = keys.next() ? keys.getInt(1) : -1;
                }
            }
        });
        int id = generatedId[0];

        if (type.isBan()) {
            String ip = CentralDatabase.query(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT ip FROM active_bans WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? rs.getString("ip") : "";
                    }
                }
            });
            String hwid = CentralDatabase.query(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT hwid FROM active_bans WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? rs.getString("hwid") : "";
                    }
                }
            });
            String finalIp = ip != null ? ip : "";
            String finalHwid = hwid != null ? hwid : "";
            String finalReason = reason;
            CentralDatabase.execute(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO active_bans (uuid, punishment_id, reason, ip, hwid, expires_at, banned_at) VALUES (?,?,?,?,?,?,?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, id);
                    ps.setString(3, finalReason);
                    ps.setString(4, finalIp);
                    ps.setString(5, finalHwid);
                    ps.setLong(6, expiresAt);
                    ps.setLong(7, now);
                    ps.executeUpdate();
                }
            });
            banCache.put(uuid, new ActiveBan(id, reason, expiresAt));
            syncBanToProxy(uuid.toString(), "ban", reason, expiresAt);
        }

        if (type == PunishmentType.MUTE) {
            CentralDatabase.execute(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO active_mutes (uuid, punishment_id, type, reason, expires_at) VALUES (?,?,?,?,?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, id);
                    ps.setString(3, "CHAT");
                    ps.setString(4, reason);
                    ps.setLong(5, expiresAt);
                    ps.executeUpdate();
                }
            });
            muteCache.put(uuid, new ActiveMute(id, "CHAT", reason, expiresAt));
        }

        if (type == PunishmentType.VOICE_MUTE) {
            CentralDatabase.execute(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO active_mutes (uuid, punishment_id, type, reason, expires_at) VALUES (?,?,?,?,?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, id);
                    ps.setString(3, "VOICE");
                    ps.setString(4, reason);
                    ps.setLong(5, expiresAt);
                    ps.executeUpdate();
                }
            });
            muteCache.put(uuid, new ActiveMute(id, "VOICE", reason, expiresAt));
        }

        return id;
    }

    public static boolean isBanned(UUID uuid) {
        ActiveBan ban = banCache.get(uuid);
        if (ban == null) return false;
        if (ban.expiresAt > 0 && System.currentTimeMillis() / 1000 > ban.expiresAt) {
            unban(uuid);
            return false;
        }
        return true;
    }

    public static boolean isBannedByHWID(String hwid) {
        return CentralDatabase.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM active_bans WHERE hwid=? AND (expires_at=0 OR expires_at>?)")) {
                ps.setString(1, hwid);
                ps.setLong(2, System.currentTimeMillis() / 1000);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public static boolean isBannedByIP(String ip) {
        return CentralDatabase.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM active_bans WHERE ip=? AND (expires_at=0 OR expires_at>?)")) {
                ps.setString(1, ip);
                ps.setLong(2, System.currentTimeMillis() / 1000);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public static boolean isMuted(UUID uuid) {
        ActiveMute mute = muteCache.get(uuid);
        if (mute == null) return false;
        if (mute.expiresAt > 0 && System.currentTimeMillis() / 1000 > mute.expiresAt) {
            unmute(uuid);
            return false;
        }
        return "CHAT".equals(mute.type) || "BOTH".equals(mute.type);
    }

    public static boolean isVoiceMuted(UUID uuid) {
        ActiveMute mute = muteCache.get(uuid);
        if (mute == null) return false;
        if (mute.expiresAt > 0 && System.currentTimeMillis() / 1000 > mute.expiresAt) {
            unmute(uuid);
            return false;
        }
        return "VOICE".equals(mute.type) || "BOTH".equals(mute.type);
    }

    public static String getMuteReason(UUID uuid) {
        ActiveMute mute = muteCache.get(uuid);
        return mute != null ? mute.reason : "";
    }

    public static String getBanReason(UUID uuid) {
        ActiveBan ban = banCache.get(uuid);
        return ban != null ? ban.reason : "";
    }

    public static boolean unban(UUID uuid) {
        if (!banCache.containsKey(uuid)) return false;
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM active_bans WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        });
        banCache.remove(uuid);
        syncBanToProxy(uuid.toString(), "unban", "", 0);
        return true;
    }

    public static boolean unmute(UUID uuid) {
        if (!muteCache.containsKey(uuid)) return false;
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM active_mutes WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        });
        muteCache.remove(uuid);
        return true;
    }

    public static void setHWID(UUID uuid, String hwid) {
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE active_bans SET hwid=? WHERE uuid=?")) {
                ps.setString(1, hwid);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        });
        ActiveBan ban = banCache.get(uuid);
        if (ban != null) {
            banCache.put(uuid, new ActiveBan(ban.punishmentId, ban.reason, ban.expiresAt));
        }
    }

    public static void setIP(UUID uuid, String ip) {
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE active_bans SET ip=? WHERE uuid=?")) {
                ps.setString(1, ip);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        });
    }

    public static void recordBanEvasion(UUID newUuid, UUID oldUuid, UUID punisherUuid) {
        issuePunishment(newUuid, punisherUuid, PunishmentType.PERM_BAN, WarnCategory.GENERAL,
            "Ban evasion (" + oldUuid + ")", 0);
        PWP.LOGGER.warn("Ban evasion detected: {} tried to evade ban of {}", newUuid, oldUuid);
    }

    public static Map<UUID, ActiveBan> getActiveBans() {
        return Collections.unmodifiableMap(new HashMap<>(banCache));
    }

    public static List<Punishment> getPunishmentHistory(UUID uuid, int limit) {
        return CentralDatabase.query(conn -> {
            List<Punishment> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM punishments WHERE uuid=? ORDER BY issued_at DESC LIMIT ?")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(resultSetToPunishment(rs));
                    }
                }
            }
            return result;
        });
    }

    public static int getActiveWarnCount(UUID uuid, WarnCategory category) {
        return CentralDatabase.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT count FROM warn_counts WHERE uuid=? AND category=?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, category.name());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("count") : 0;
                }
            }
        });
    }

    public static void incrementWarnCount(UUID uuid, WarnCategory category) {
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO warn_counts (uuid, category, count, last_warn_at) VALUES (?,?,1,?) " +
                    "ON CONFLICT(uuid,category) DO UPDATE SET count=count+1, last_warn_at=?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, category.name());
                ps.setLong(3, System.currentTimeMillis() / 1000);
                ps.setLong(4, System.currentTimeMillis() / 1000);
                ps.executeUpdate();
            }
        });
    }

    private static void syncBanToProxy(String uuid, String action, String reason, long expiresAt) {
        if (ProxyMessenger.server != null) {
            ProxyMessenger.send("ban_sync:" + action + ":" + uuid + ":" + reason + ":" + expiresAt);
        }
    }

    private static Punishment resultSetToPunishment(ResultSet rs) throws SQLException {
        return new Punishment(
            rs.getInt("id"),
            UUID.fromString(rs.getString("uuid")),
            UUID.fromString(rs.getString("punisher_uuid")),
            PunishmentType.valueOf(rs.getString("type")),
            rs.getString("category") != null && !rs.getString("category").isEmpty()
                ? WarnCategory.valueOf(rs.getString("category")) : null,
            rs.getString("reason"),
            rs.getLong("duration_seconds"),
            rs.getLong("issued_at"),
            rs.getLong("expires_at"),
            rs.getInt("active") == 1,
            rs.getString("server_id")
        );
    }
}
