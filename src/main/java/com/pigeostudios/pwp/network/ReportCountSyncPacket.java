package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.gui.overlay.ReportHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReportCountSyncPacket {
    private final int count;

    public ReportCountSyncPacket(int count) {
        this.count = count;
    }

    public ReportCountSyncPacket(FriendlyByteBuf buf) {
        this.count = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(count);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ReportHudOverlay.setPendingCount(count);
            });
        });
        return true;
    }
}
