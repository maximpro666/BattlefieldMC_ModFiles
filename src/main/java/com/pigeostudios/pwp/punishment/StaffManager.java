package com.pigeostudios.pwp.punishment;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.data.CentralDatabase;
import net.minecraft.server.level.ServerPlayer;

import java.sql.*;
import java.util.*;

public class StaffManager {
    private static final Map<UUID, StaffRole> staffCache = new HashMap<>();
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        CentralDatabase.execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS staff_roles (
                        uuid TEXT PRIMARY KEY,
                        role TEXT NOT NULL,
                        assigned_by TEXT NOT NULL,
                        assigned_at INTEGER NOT NULL
                    )
                """);
            }
        });
        loadAll();
        initialized = true;
        PWP.LOGGER.info("StaffManager initialized");
    }

    private static void loadAll() {
        staffCache.clear();
        CentralDatabase.execute(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT uuid, role FROM staff_roles")) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    StaffRole role = StaffRole.valueOf(rs.getString("role"));
                    staffCache.put(uuid, role);
                }
            }
        });
    }

    public static StaffRole getRole(UUID uuid) {
        StaffRole cached = staffCache.get(uuid);
        if (cached != null) return cached;
        StaffRole dbRole = CentralDatabase.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT role FROM staff_roles WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return StaffRole.valueOf(rs.getString("role"));
                    return null;
                }
            }
        });
        if (dbRole != null) {
            staffCache.put(uuid, dbRole);
        }
        return dbRole;
    }

    public static boolean isStaff(UUID uuid) {
        return getRole(uuid) != null;
    }

    public static boolean hasPermission(ServerPlayer player, StaffRole required) {
        StaffRole role = getRole(player.getUUID());
        if (role != null && role.ordinal() >= required.ordinal()) return true;
        return switch (required) {
            case MOD -> player.hasPermissions(2);
            case ADMIN -> player.hasPermissions(2);
            case OWNER -> player.hasPermissions(4);
        };
    }

    public static boolean hasPermission(ServerPlayer player, PunishmentType type) {
        StaffRole role = getRole(player.getUUID());
        if (role != null) return role.canUse(type);
        return switch (type) {
            case WARN -> player.hasPermissions(2);
            case KICK, MUTE, VOICE_MUTE, TEMP_BAN, PERM_BAN -> player.hasPermissions(2);
        };
    }

    public static boolean setRole(UUID uuid, StaffRole role, UUID assignedBy) {
        StaffRole existing = getRole(uuid);
        if (existing == role) return false;
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO staff_roles (uuid, role, assigned_by, assigned_at) VALUES (?,?,?,?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, role.name());
                ps.setString(3, assignedBy.toString());
                ps.setLong(4, System.currentTimeMillis() / 1000);
                ps.executeUpdate();
            }
        });
        staffCache.put(uuid, role);
        return true;
    }

    public static boolean removeRole(UUID uuid) {
        StaffRole existing = getRole(uuid);
        if (existing == null) return false;
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM staff_roles WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        });
        staffCache.remove(uuid);
        return true;
    }

    public static Map<UUID, StaffRole> getAllStaff() {
        return Collections.unmodifiableMap(new HashMap<>(staffCache));
    }
}
