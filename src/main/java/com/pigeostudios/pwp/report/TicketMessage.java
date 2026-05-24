package com.pigeostudios.pwp.report;

import java.util.UUID;

public class TicketMessage {
    private final int id;
    private final int ticketId;
    private final UUID senderUuid;
    private final String senderName;
    private final String message;
    private final long sentAt;
    private final boolean system;

    public TicketMessage(int id, int ticketId, UUID senderUuid, String senderName,
                         String message, long sentAt, boolean system) {
        this.id = id;
        this.ticketId = ticketId;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.message = message;
        this.sentAt = sentAt;
        this.system = system;
    }

    public int getId() { return id; }
    public int getTicketId() { return ticketId; }
    public UUID getSenderUuid() { return senderUuid; }
    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
    public long getSentAt() { return sentAt; }
    public boolean isSystem() { return system; }
}
