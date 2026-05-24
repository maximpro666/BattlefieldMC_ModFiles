package com.pigeostudios.pwp.report;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.data.CentralDatabase;
import com.pigeostudios.pwp.network.NotificationPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.PacketDistributor;

import java.sql.*;
import java.util.*;

public class ReportManager {
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        CentralDatabase.execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        reporter_uuid TEXT NOT NULL,
                        target_uuid TEXT NOT NULL,
                        target_name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        description TEXT DEFAULT '',
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        assigned_to TEXT DEFAULT NULL,
                        punishment_id INTEGER DEFAULT NULL,
                        created_at INTEGER NOT NULL,
                        resolved_at INTEGER DEFAULT NULL,
                        resolved_by TEXT DEFAULT NULL
                    )
                """);
            }
        });
        initialized = true;
        PWP.LOGGER.info("ReportManager initialized");
    }

    public static int createReport(UUID reporterUuid, UUID targetUuid, String targetName,
                                    ReportType type, String description) {
        int[] generatedId = new int[1];
        long now = System.currentTimeMillis() / 1000;
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO reports (reporter_uuid, target_uuid, target_name, type, description, status, created_at) VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, reporterUuid.toString());
                ps.setString(2, targetUuid.toString());
                ps.setString(3, targetName);
                ps.setString(4, type.name());
                ps.setString(5, description != null ? description : "");
                ps.setString(6, ReportStatus.PENDING.name());
                ps.setLong(7, now);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    generatedId[0] = keys.next() ? keys.getInt(1) : -1;
                }
            }
        });
        return generatedId[0];
    }

    public static void notifyStaff(MinecraftServer server, int reportId, String reporterName, String targetName, ReportType type) {
        if (server == null) return;
        String msg = "§c[Reports] §7#" + reportId + " §f" + reporterName + " §7→ §f" + targetName
            + " §7(" + type.getDisplayName() + ")";
        for (var player : server.getPlayerList().getPlayers()) {
            if (com.pigeostudios.pwp.punishment.StaffManager.isStaff(player.getUUID())) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
                PacketHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new NotificationPacket("§cNew Report #" + reportId, "warning", 5000, ""));
            }
        }
    }

    public static List<Report> getPendingReports() {
        return getReportsByStatus(ReportStatus.PENDING);
    }

    public static List<Report> getReportsByStatus(ReportStatus status) {
        return CentralDatabase.query(conn -> {
            List<Report> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM reports WHERE status=? ORDER BY created_at ASC")) {
                ps.setString(1, status.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(resultSetToReport(rs));
                    }
                }
            }
            return result;
        });
    }

    public static List<Report> getReportsByTarget(UUID targetUuid) {
        return CentralDatabase.query(conn -> {
            List<Report> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM reports WHERE target_uuid=? ORDER BY created_at DESC LIMIT 50")) {
                ps.setString(1, targetUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(resultSetToReport(rs));
                    }
                }
            }
            return result;
        });
    }

    public static Report getReport(int id) {
        return CentralDatabase.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM reports WHERE id=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? resultSetToReport(rs) : null;
                }
            }
        });
    }

    public static boolean claimReport(int id, UUID modUuid) {
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reports SET status=?, assigned_to=? WHERE id=? AND status='PENDING'")) {
                ps.setString(1, ReportStatus.UNDER_REVIEW.name());
                ps.setString(2, modUuid.toString());
                ps.setInt(3, id);
                ps.executeUpdate();
            }
        });
        return true;
    }

    public static boolean dismissReport(int id, UUID resolvedBy, String reason) {
        long now = System.currentTimeMillis() / 1000;
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reports SET status=?, resolved_at=?, resolved_by=? WHERE id=?")) {
                ps.setString(1, ReportStatus.DISMISSED.name());
                ps.setLong(2, now);
                ps.setString(3, resolvedBy.toString());
                ps.setInt(4, id);
                ps.executeUpdate();
            }
        });
        return true;
    }

    public static boolean linkPunishment(int reportId, int punishmentId, UUID resolvedBy) {
        long now = System.currentTimeMillis() / 1000;
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reports SET status=?, punishment_id=?, resolved_at=?, resolved_by=? WHERE id=?")) {
                ps.setString(1, ReportStatus.ACTION_TAKEN.name());
                ps.setInt(2, punishmentId);
                ps.setLong(3, now);
                ps.setString(4, resolvedBy.toString());
                ps.setInt(5, reportId);
                ps.executeUpdate();
            }
        });
        return true;
    }

    public static int getPendingCount() {
        return CentralDatabase.query(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reports WHERE status='PENDING'")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public static List<Report> getAllReports() {
        return CentralDatabase.query(conn -> {
            List<Report> result = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM reports ORDER BY created_at DESC LIMIT 100")) {
                while (rs.next()) {
                    result.add(resultSetToReport(rs));
                }
            }
            return result;
        });
    }

    public static List<Report> getReportsByReporter(UUID reporterUuid) {
        return CentralDatabase.query(conn -> {
            List<Report> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM reports WHERE reporter_uuid=? ORDER BY created_at DESC LIMIT 20")) {
                ps.setString(1, reporterUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(resultSetToReport(rs));
                    }
                }
            }
            return result;
        });
    }

    // ===== Ticket Messaging =====

    public static void addTicketMessage(int ticketId, UUID senderUuid, String senderName, String message) {
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ticket_messages (ticket_id, sender_uuid, sender_name, message, sent_at) VALUES (?,?,?,?,?)")) {
                ps.setInt(1, ticketId);
                ps.setString(2, senderUuid.toString());
                ps.setString(3, senderName);
                ps.setString(4, message);
                ps.setLong(5, System.currentTimeMillis() / 1000);
                ps.executeUpdate();
            }
        });
    }

    public static void addSystemMessage(int ticketId, String message) {
        CentralDatabase.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ticket_messages (ticket_id, sender_uuid, sender_name, message, sent_at, is_system) VALUES (?,?,?,?,?,1)")) {
                ps.setInt(1, ticketId);
                ps.setString(2, UUID.randomUUID().toString());
                ps.setString(3, "System");
                ps.setString(4, message);
                ps.setLong(5, System.currentTimeMillis() / 1000);
                ps.executeUpdate();
            }
        });
    }

    public static List<TicketMessage> getTicketMessages(int ticketId) {
        return CentralDatabase.query(conn -> {
            List<TicketMessage> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM ticket_messages WHERE ticket_id=? ORDER BY sent_at ASC")) {
                ps.setInt(1, ticketId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(new TicketMessage(
                            rs.getInt("id"),
                            rs.getInt("ticket_id"),
                            UUID.fromString(rs.getString("sender_uuid")),
                            rs.getString("sender_name"),
                            rs.getString("message"),
                            rs.getLong("sent_at"),
                            rs.getInt("is_system") == 1
                        ));
                    }
                }
            }
            return result;
        });
    }

    private static Report resultSetToReport(ResultSet rs) throws SQLException {
        String assignedStr = rs.getString("assigned_to");
        String resolvedByStr = rs.getString("resolved_by");
        Long resolvedAt = rs.getObject("resolved_at") != null ? rs.getLong("resolved_at") : null;
        Integer punishmentId = rs.getObject("punishment_id") != null ? rs.getInt("punishment_id") : null;

        return new Report(
            rs.getInt("id"),
            UUID.fromString(rs.getString("reporter_uuid")),
            UUID.fromString(rs.getString("target_uuid")),
            rs.getString("target_name"),
            ReportType.valueOf(rs.getString("type")),
            rs.getString("description"),
            ReportStatus.valueOf(rs.getString("status")),
            assignedStr != null ? UUID.fromString(assignedStr) : null,
            punishmentId,
            rs.getLong("created_at"),
            resolvedAt,
            resolvedByStr != null ? UUID.fromString(resolvedByStr) : null
        );
    }
}
