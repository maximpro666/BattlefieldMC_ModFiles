package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.core.DownedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class DownedSyncPacket {
    private List<DownedData> downedList;

    public DownedSyncPacket(List<DownedData> downedList) {
        this.downedList = downedList != null ? downedList : new ArrayList<>();
    }

    public DownedSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.downedList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String playerName = buf.readUtf(256);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            String dimension = buf.readUtf(256);
            boolean hasDowner = buf.readBoolean();
            UUID downerUUID = hasDowner ? buf.readUUID() : null;
            int bleedoutTimer = buf.readInt();
            double reviveProgress = buf.readDouble();

            DownedData data = new DownedData(null, playerName, x, y, z, dimension, downerUUID);
            downedList.add(data);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(downedList.size());
        for (DownedData d : downedList) {
            buf.writeUtf(d.getPlayerName() != null ? d.getPlayerName() : "");
            buf.writeDouble(d.getX());
            buf.writeDouble(d.getY());
            buf.writeDouble(d.getZ());
            buf.writeUtf(d.getDimension() != null ? d.getDimension() : "");
            buf.writeBoolean(d.getDownerUUID() != null);
            if (d.getDownerUUID() != null) buf.writeUUID(d.getDownerUUID());
            buf.writeInt(d.getBleedoutTimer());
            buf.writeDouble(d.getReviveProgress());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide(downedList));
        });
        return true;
    }

    private static void handleClientSide(List<DownedData> downedList) {
        com.yourmod.teamsystem.client.ClientTeamData.downedPlayers = downedList;
    }

    public List<DownedData> getDownedList() { return downedList; }
}
