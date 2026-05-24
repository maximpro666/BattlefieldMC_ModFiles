package com.pigeostudios.pwp.punishment;

import java.util.UUID;

public class Punishment {
    private final int id;
    private final UUID uuid;
    private final UUID punisherUuid;
    private final PunishmentType type;
    private final WarnCategory category;
    private final String reason;
    private final long durationSeconds;
    private final long issuedAt;
    private final long expiresAt;
    private final boolean active;
    private final String serverId;

    public Punishment(int id, UUID uuid, UUID punisherUuid, PunishmentType type,
                      WarnCategory category, String reason, long durationSeconds,
                      long issuedAt, long expiresAt, boolean active, String serverId) {
        this.id = id;
        this.uuid = uuid;
        this.punisherUuid = punisherUuid;
        this.type = type;
        this.category = category;
        this.reason = reason;
        this.durationSeconds = durationSeconds;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.serverId = serverId;
    }

    public int getId() { return id; }
    public UUID getUuid() { return uuid; }
    public UUID getPunisherUuid() { return punisherUuid; }
    public PunishmentType getType() { return type; }
    public WarnCategory getCategory() { return category; }
    public String getReason() { return reason; }
    public long getDurationSeconds() { return durationSeconds; }
    public long getIssuedAt() { return issuedAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isActive() { return active; }
    public String getServerId() { return serverId; }

    public boolean isExpired() {
        if (type.isPermanent()) return false;
        return System.currentTimeMillis() / 1000 > expiresAt;
    }

    public long getRemainingSeconds() {
        if (type.isPermanent()) return -1;
        return Math.max(0, expiresAt - System.currentTimeMillis() / 1000);
    }
}
