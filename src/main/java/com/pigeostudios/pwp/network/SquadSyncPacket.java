package com.pigeostudios.pwp.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class SquadSyncPacket {
    private int playerSquadId;
    private String playerSquadName;

    public SquadSyncPacket(int playerSquadId, String playerSquadName) {
        this.playerSquadId = playerSquadId;
        this.playerSquadName = playerSquadName;
    }

    public SquadSyncPacket(FriendlyByteBuf buf) {
        this.playerSquadId = buf.readInt();
        this.playerSquadName = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerSquadId);
        buf.writeUtf(playerSquadName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.pigeostudios.pwp.client.ClientTeamData.localPlayerSquad = playerSquadName);
        });
        return true;
    }

    public int getPlayerSquadId() { return playerSquadId; }
    public String getPlayerSquadName() { return playerSquadName; }
}
