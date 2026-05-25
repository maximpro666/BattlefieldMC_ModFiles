package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.report.Report;
import com.pigeostudios.pwp.report.ReportStatus;
import com.pigeostudios.pwp.report.ReportType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;

public class TicketListSyncPacket {
    private final List<TicketEntry> tickets;

    public static final class TicketEntry {
        public final int id;
        public final String reporterName;
        public final String targetName;
        public final String type;
        public final String status;
        public final String assignedTo;
        public final long createdAt;

        public TicketEntry(int id, String reporterName, String targetName, String type,
                           String status, String assignedTo, long createdAt) {
            this.id = id; this.reporterName = reporterName; this.targetName = targetName;
            this.type = type; this.status = status; this.assignedTo = assignedTo;
            this.createdAt = createdAt;
        }
    }

    public TicketListSyncPacket(List<Report> reports, MinecraftServer server) {
        this.tickets = new ArrayList<>();
        for (Report r : reports) {
            String reporterName = r.getTargetName();
            if (server != null) {
                var reporterPlayer = server.getPlayerList().getPlayer(r.getReporterUuid());
                reporterName = reporterPlayer != null ? reporterPlayer.getName().getString() : r.getReporterUuid().toString().substring(0, 8);
            }
            tickets.add(new TicketEntry(
                r.getId(),
                reporterName,
                r.getTargetName(),
                r.getType().getDisplayName(),
                r.getStatus().getDisplayName(),
                r.getAssignedTo() != null ? r.getAssignedTo().toString() : "",
                r.getCreatedAt()
            ));
        }
    }

    public TicketListSyncPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.tickets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tickets.add(new TicketEntry(
                buf.readInt(),
                buf.readUtf(128),
                buf.readUtf(64),
                buf.readUtf(32),
                buf.readUtf(32),
                buf.readUtf(64),
                buf.readLong()
            ));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(tickets.size());
        for (TicketEntry t : tickets) {
            buf.writeInt(t.id);
            buf.writeUtf(t.reporterName);
            buf.writeUtf(t.targetName);
            buf.writeUtf(t.type);
            buf.writeUtf(t.status);
            buf.writeUtf(t.assignedTo);
            buf.writeLong(t.createdAt);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTeamData.ticketList = this.tickets;
                com.pigeostudios.pwp.client.gui.overlay.ReportHudOverlay.setPendingCount(
                    (int) tickets.stream().filter(t -> "Pending".equals(t.status)).count());
            });
        });
        return true;
    }

    public List<TicketEntry> getTickets() { return tickets; }
}
