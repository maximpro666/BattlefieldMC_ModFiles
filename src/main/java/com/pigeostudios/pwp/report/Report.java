package com.pigeostudios.pwp.report;

import java.util.UUID;

public class Report {
    private final int id;
    private final UUID reporterUuid;
    private final UUID targetUuid;
    private final String targetName;
    private final ReportType type;
    private final String description;
    private final ReportStatus status;
    private final UUID assignedTo;
    private final Integer punishmentId;
    private final long createdAt;
    private final Long resolvedAt;
    private final UUID resolvedBy;

    public Report(int id, UUID reporterUuid, UUID targetUuid, String targetName,
                  ReportType type, String description, ReportStatus status,
                  UUID assignedTo, Integer punishmentId, long createdAt,
                  Long resolvedAt, UUID resolvedBy) {
        this.id = id;
        this.reporterUuid = reporterUuid;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.type = type;
        this.description = description;
        this.status = status;
        this.assignedTo = assignedTo;
        this.punishmentId = punishmentId;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
    }

    public int getId() { return id; }
    public UUID getReporterUuid() { return reporterUuid; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public ReportType getType() { return type; }
    public String getDescription() { return description; }
    public ReportStatus getStatus() { return status; }
    public UUID getAssignedTo() { return assignedTo; }
    public Integer getPunishmentId() { return punishmentId; }
    public long getCreatedAt() { return createdAt; }
    public Long getResolvedAt() { return resolvedAt; }
    public UUID getResolvedBy() { return resolvedBy; }

    public Report withStatus(ReportStatus newStatus) {
        return new Report(id, reporterUuid, targetUuid, targetName, type, description,
                newStatus, assignedTo, punishmentId, createdAt, resolvedAt, resolvedBy);
    }

    public Report withAssigned(UUID modUuid) {
        return new Report(id, reporterUuid, targetUuid, targetName, type, description,
                status, modUuid, punishmentId, createdAt, resolvedAt, resolvedBy);
    }

    public Report withResolution(ReportStatus newStatus, UUID resolver, Integer punishmentId) {
        return new Report(id, reporterUuid, targetUuid, targetName, type, description,
                newStatus, assignedTo, punishmentId, createdAt,
                System.currentTimeMillis() / 1000, resolver);
    }
}
