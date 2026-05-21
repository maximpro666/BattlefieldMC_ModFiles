package com.pigeostudios.pwp.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WCSyncPacket {
    private int warCredits;

    public WCSyncPacket(int warCredits) {
        this.warCredits = warCredits;
    }

    public WCSyncPacket(FriendlyByteBuf buf) {
        this.warCredits = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(warCredits);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide(warCredits));
        });
        return true;
    }

    private static void handleClientSide(int warCredits) {
        com.pigeostudios.pwp.client.ClientTeamData.localPlayerWC = warCredits;
    }
}
