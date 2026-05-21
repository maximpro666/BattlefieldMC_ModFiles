package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RankSyncPacket {
    private int rankOrdinal;
    private boolean isRussian;

    public RankSyncPacket(int rankOrdinal, boolean isRussian) {
        this.rankOrdinal = rankOrdinal;
        this.isRussian = isRussian;
    }

    public RankSyncPacket(FriendlyByteBuf buf) {
        this.rankOrdinal = buf.readInt();
        this.isRussian = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(rankOrdinal);
        buf.writeBoolean(isRussian);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide(rankOrdinal, isRussian));
        });
        return true;
    }

    private static void handleClientSide(int rankOrdinal, boolean isRussian) {
        ClientTeamData.localPlayerRank = rankOrdinal;
    }

    public int getRankOrdinal() {
        return rankOrdinal;
    }

    public boolean isRussian() {
        return isRussian;
    }
}
