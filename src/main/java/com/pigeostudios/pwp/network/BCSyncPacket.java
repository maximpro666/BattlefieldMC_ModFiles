package com.pigeostudios.pwp.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BCSyncPacket {
    private int battleCredits;

    public BCSyncPacket(int battleCredits) {
        this.battleCredits = battleCredits;
    }

    public BCSyncPacket(FriendlyByteBuf buf) {
        this.battleCredits = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(battleCredits);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide(battleCredits));
        });
        return true;
    }

    private static void handleClientSide(int battleCredits) {
        com.pigeostudios.pwp.client.ClientTeamData.localPlayerBC = battleCredits;
    }
}
