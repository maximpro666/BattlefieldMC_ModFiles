package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SPSyncPacket {
    private int scorePoints;

    public SPSyncPacket(int scorePoints) {
        this.scorePoints = scorePoints;
    }

    public SPSyncPacket(FriendlyByteBuf buf) {
        this.scorePoints = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(scorePoints);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide(scorePoints));
        });
        return true;
    }

    private static void handleClientSide(int scorePoints) {
        com.yourmod.teamsystem.client.ClientTeamData.localPlayerSP = scorePoints;
    }
}
