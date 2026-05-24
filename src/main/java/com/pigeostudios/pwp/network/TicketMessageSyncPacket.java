package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class TicketMessageSyncPacket {
    private final int ticketId;
    private final String senderName;
    private final String message;

    public TicketMessageSyncPacket(int ticketId, String senderName, String message) {
        this.ticketId = ticketId;
        this.senderName = senderName;
        this.message = message;
    }

    public TicketMessageSyncPacket(FriendlyByteBuf buf) {
        this.ticketId = buf.readInt();
        this.senderName = buf.readUtf(64);
        this.message = buf.readUtf(512);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(ticketId);
        buf.writeUtf(senderName);
        buf.writeUtf(message);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.pigeostudios.pwp.client.gui.overlay.ReportHudOverlay.onNewMessage(ticketId, senderName, message);
            });
        });
        return true;
    }
}
