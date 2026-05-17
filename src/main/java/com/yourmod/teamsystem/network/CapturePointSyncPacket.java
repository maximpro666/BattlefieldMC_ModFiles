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
    private List<Integer> capturingTeamOrdinals;

    public CapturePointSyncPacket(List<Integer> pointIds, List<Double> progressPercentages,
                                  List<Integer> ownerTeamOrdinals, List<String> pointNames,
                                  List<Integer> capturingTeamOrdinals) {
        this.pointIds = pointIds;
        this.progressPercentages = progressPercentages;
        this.ownerTeamOrdinals = ownerTeamOrdinals;
        this.pointNames = pointNames;
        this.capturingTeamOrdinals = capturingTeamOrdinals;
    }

    public CapturePointSyncPacket(FriendlyByteBuf buf) {
        int pointCount = buf.readInt();
        this.pointIds = new ArrayList<>();
        this.progressPercentages = new ArrayList<>();
        this.ownerTeamOrdinals = new ArrayList<>();
        this.pointNames = new ArrayList<>();
        this.capturingTeamOrdinals = new ArrayList<>();

        for (int i = 0; i < pointCount; i++) {
            pointIds.add(buf.readInt());
            progressPercentages.add(buf.readDouble());
            ownerTeamOrdinals.add(buf.readInt());
            pointNames.add(buf.readUtf(256));
            capturingTeamOrdinals.add(buf.readInt());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(pointIds.size());
        for (int i = 0; i < pointIds.size(); i++) {
            buf.writeInt(pointIds.get(i));
            buf.writeDouble(progressPercentages.get(i));
            buf.writeInt(ownerTeamOrdinals.get(i));
            buf.writeUtf(pointNames.get(i));
            buf.writeInt(capturingTeamOrdinals.get(i));
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                handleClientSide(pointIds, progressPercentages, ownerTeamOrdinals, pointNames, capturingTeamOrdinals));
        });
        return true;
    }

    private static void handleClientSide(List<Integer> pointIds, List<Double> progressPercentages,
                                         List<Integer> ownerTeamOrdinals, List<String> pointNames,
                                         List<Integer> capturingTeamOrdinals) {
    }

    public List<Integer> getPointIds() { return pointIds; }
    public List<Double> getProgressPercentages() { return progressPercentages; }
    public List<Integer> getOwnerTeamOrdinals() { return ownerTeamOrdinals; }
    public List<String> getPointNames() { return pointNames; }
    public List<Integer> getCapturingTeamOrdinals() { return capturingTeamOrdinals; }
}
