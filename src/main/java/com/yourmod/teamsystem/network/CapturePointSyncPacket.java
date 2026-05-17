package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class CapturePointSyncPacket {
    private List<Integer> pointIds;
    private List<Double> progressPercentages;
    private List<Integer> ownerTeamOrdinals;
    private List<String> pointNames;

    public CapturePointSyncPacket(List<Integer> pointIds, List<Double> progressPercentages,
                                 List<Integer> ownerTeamOrdinals, List<String> pointNames) {
        this.pointIds = pointIds;
        this.progressPercentages = progressPercentages;
        this.ownerTeamOrdinals = ownerTeamOrdinals;
        this.pointNames = pointNames;
    }

    public CapturePointSyncPacket(FriendlyByteBuf buf) {
        int pointCount = buf.readInt();
        this.pointIds = new ArrayList<>();
        this.progressPercentages = new ArrayList<>();
        this.ownerTeamOrdinals = new ArrayList<>();
        this.pointNames = new ArrayList<>();

        for (int i = 0; i < pointCount; i++) {
            pointIds.add(buf.readInt());
            progressPercentages.add(buf.readDouble());
            ownerTeamOrdinals.add(buf.readInt());
            pointNames.add(buf.readUtf(256));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(pointIds.size());
        for (int i = 0; i < pointIds.size(); i++) {
            buf.writeInt(pointIds.get(i));
            buf.writeDouble(progressPercentages.get(i));
            buf.writeInt(ownerTeamOrdinals.get(i));
            buf.writeUtf(pointNames.get(i));
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                handleClientSide(pointIds, progressPercentages, ownerTeamOrdinals, pointNames));
        });
        return true;
    }

    private static void handleClientSide(List<Integer> pointIds, List<Double> progressPercentages,
                                        List<Integer> ownerTeamOrdinals, List<String> pointNames) {
        // Client-side capture point update
    }

    public List<Integer> getPointIds() { return pointIds; }
    public List<Double> getProgressPercentages() { return progressPercentages; }
    public List<Integer> getOwnerTeamOrdinals() { return ownerTeamOrdinals; }
    public List<String> getPointNames() { return pointNames; }
}
