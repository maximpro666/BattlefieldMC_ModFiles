package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RankSyncPacket {
    private int rankOrdinal;

    public RankSyncPacket(int rankOrdinal) {
        this.rankOrdinal = rankOrdinal;
    }

    public RankSyncPacket(FriendlyByteBuf buf) {
        this.rankOrdinal = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(rankOrdinal);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide(rankOrdinal));
        });
        return true;
    }

    private static void handleClientSide(int rankOrdinal) {
        com.yourmod.teamsystem.client.ClientTeamData.localPlayerRank = rankOrdinal;
    }

    public int getRankOrdinal() {
        return rankOrdinal;
    }
}
