package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.report.ReportManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class TicketClaimPacket {
    private final int ticketId;
    private final boolean claim;

    public TicketClaimPacket(int ticketId, boolean claim) {
        this.ticketId = ticketId;
        this.claim = claim;
    }

    public TicketClaimPacket(FriendlyByteBuf buf) {
        this.ticketId = buf.readInt();
        this.claim = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(ticketId);
        buf.writeBoolean(claim);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!com.pigeostudios.pwp.punishment.StaffManager.isStaff(player.getUUID())) return;

            if (claim) {
                ReportManager.claimReport(ticketId, player.getUUID());
                ReportManager.addSystemMessage(ticketId, "§a" + player.getName().getString() + " взял тикет");
            }
        });
        return true;
    }
}
