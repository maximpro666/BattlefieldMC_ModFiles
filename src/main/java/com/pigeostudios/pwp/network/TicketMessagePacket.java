package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.report.*;
import com.pigeostudios.pwp.punishment.StaffManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class TicketMessagePacket {
    private final int ticketId;
    private final String message;

    public TicketMessagePacket(int ticketId, String message) {
        this.ticketId = ticketId;
        this.message = message != null ? message : "";
    }

    public TicketMessagePacket(FriendlyByteBuf buf) {
        this.ticketId = buf.readInt();
        this.message = buf.readUtf(512);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(ticketId);
        buf.writeUtf(message);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || message.isEmpty()) return;

            Report report = ReportManager.getReport(ticketId);
            if (report == null) return;

            boolean isStaff = StaffManager.isStaff(player.getUUID());
            boolean isReporter = report.getReporterUuid().equals(player.getUUID());

            if (!isStaff && !isReporter) return;

            ReportManager.addTicketMessage(ticketId, player.getUUID(), player.getName().getString(), message);

            ServerPlayer targetPlayer = null;
            if (isStaff && report.getReporterUuid() != null) {
                targetPlayer = player.getServer().getPlayerList().getPlayer(report.getReporterUuid());
            } else if (isReporter && report.getAssignedTo() != null) {
                targetPlayer = player.getServer().getPlayerList().getPlayer(report.getAssignedTo());
            }

            if (targetPlayer != null && targetPlayer != player) {
                final ServerPlayer finalTarget = targetPlayer;
                PacketHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> finalTarget),
                    new TicketMessageSyncPacket(ticketId, player.getName().getString(), message));
            }
        });
        return true;
    }
}
