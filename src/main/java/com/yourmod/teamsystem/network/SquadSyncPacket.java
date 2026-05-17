package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class SquadSyncPacket {
    private int playerSquadId;
    private String playerSquadName;
    private List<Integer> squadIds;
    private List<String> squadNames;
    private List<Integer> squadSizes;

    public SquadSyncPacket(int playerSquadId, String playerSquadName,
                          List<Integer> squadIds, List<String> squadNames, List<Integer> squadSizes) {
        this.playerSquadId = playerSquadId;
        this.playerSquadName = playerSquadName;
        this.squadIds = squadIds;
        this.squadNames = squadNames;
        this.squadSizes = squadSizes;
    }

    public SquadSyncPacket(FriendlyByteBuf buf) {
        this.playerSquadId = buf.readInt();
        this.playerSquadName = buf.readUtf(256);
        int squadCount = buf.readInt();
        this.squadIds = new ArrayList<>();
        this.squadNames = new ArrayList<>();
        this.squadSizes = new ArrayList<>();
        for (int i = 0; i < squadCount; i++) {
            squadIds.add(buf.readInt());
            squadNames.add(buf.readUtf(256));
            squadSizes.add(buf.readInt());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerSquadId);
        buf.writeUtf(playerSquadName);
        buf.writeInt(squadIds.size());
        for (int i = 0; i < squadIds.size(); i++) {
            buf.writeInt(squadIds.get(i));
            buf.writeUtf(squadNames.get(i));
            buf.writeInt(squadSizes.get(i));
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                handleClientSide(playerSquadId, playerSquadName, squadIds, squadNames, squadSizes));
        });
        return true;
    }

    private static void handleClientSide(int playerSquadId, String playerSquadName,
                                        List<Integer> squadIds, List<String> squadNames, List<Integer> squadSizes) {
        com.yourmod.teamsystem.client.ClientTeamData.localPlayerSquad = playerSquadName;
    }

    public int getPlayerSquadId() { return playerSquadId; }
    public String getPlayerSquadName() { return playerSquadName; }
    public List<Integer> getSquadIds() { return squadIds; }
    public List<String> getSquadNames() { return squadNames; }
    public List<Integer> getSquadSizes() { return squadSizes; }
}
