package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.CapturePointData;
import com.pigeostudios.pwp.client.ClientMarkerData;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.core.MarkerData;
import com.pigeostudios.pwp.core.MarkerData.MarkerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

import java.util.*;
import java.util.function.Supplier;

public class CapturePointSyncPacket {
    private List<Integer> pointIds;
    private List<Double> progressPercentages;
    private List<Integer> ownerTeamOrdinals;
    private List<String> pointNames;
    private List<Integer> capturingTeamOrdinals;
    private List<Double> pointXs;
    private List<Double> pointYs;
    private List<Double> pointZs;
    private List<Double> pointRadii;
    private List<String> pointTypes;

    public CapturePointSyncPacket(List<Integer> pointIds, List<Double> progressPercentages,
                                  List<Integer> ownerTeamOrdinals, List<String> pointNames,
                                  List<Integer> capturingTeamOrdinals,
                                  List<Double> pointXs, List<Double> pointYs, List<Double> pointZs,
                                  List<Double> pointRadii, List<String> pointTypes) {
        this.pointIds = pointIds;
        this.progressPercentages = progressPercentages;
        this.ownerTeamOrdinals = ownerTeamOrdinals;
        this.pointNames = pointNames;
        this.capturingTeamOrdinals = capturingTeamOrdinals;
        this.pointXs = pointXs;
        this.pointYs = pointYs;
        this.pointZs = pointZs;
        this.pointRadii = pointRadii;
        this.pointTypes = pointTypes;
    }

    public CapturePointSyncPacket(FriendlyByteBuf buf) {
        int pointCount = buf.readInt();
        this.pointIds = new ArrayList<>();
        this.progressPercentages = new ArrayList<>();
        this.ownerTeamOrdinals = new ArrayList<>();
        this.pointNames = new ArrayList<>();
        this.capturingTeamOrdinals = new ArrayList<>();
        this.pointXs = new ArrayList<>();
        this.pointYs = new ArrayList<>();
        this.pointZs = new ArrayList<>();
        this.pointRadii = new ArrayList<>();
        this.pointTypes = new ArrayList<>();

        for (int i = 0; i < pointCount; i++) {
            pointIds.add(buf.readInt());
            progressPercentages.add(buf.readDouble());
            ownerTeamOrdinals.add(buf.readInt());
            pointNames.add(buf.readUtf(256));
            capturingTeamOrdinals.add(buf.readInt());
            pointXs.add(buf.readDouble());
            pointYs.add(buf.readDouble());
            pointZs.add(buf.readDouble());
            pointRadii.add(buf.readDouble());
            pointTypes.add(buf.readUtf(16));
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
            buf.writeDouble(pointXs.get(i));
            buf.writeDouble(pointYs.get(i));
            buf.writeDouble(pointZs.get(i));
            buf.writeDouble(pointRadii.get(i));
            buf.writeUtf(pointTypes.get(i));
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                handleClientSide(pointIds, progressPercentages, ownerTeamOrdinals, pointNames, capturingTeamOrdinals, pointXs, pointYs, pointZs, pointRadii, pointTypes));
        });
        return true;
    }

    private static void handleClientSide(List<Integer> pointIds, List<Double> progressPercentages,
                                          List<Integer> ownerTeamOrdinals, List<String> pointNames,
                                          List<Integer> capturingTeamOrdinals,
                                          List<Double> pointXs, List<Double> pointYs, List<Double> pointZs,
                                          List<Double> pointRadii, List<String> pointTypes) {
        List<CapturePointData> points = new ArrayList<>();
        for (int i = 0; i < pointIds.size(); i++) {
            points.add(new CapturePointData(
                pointIds.get(i), progressPercentages.get(i),
                ownerTeamOrdinals.get(i), pointNames.get(i),
                capturingTeamOrdinals.get(i),
                pointXs.get(i), pointYs.get(i), pointZs.get(i),
                pointRadii.get(i), pointTypes.get(i)));
        }
        ClientTeamData.capturePoints = points;
        com.pigeostudios.pwp.client.journeymap.JourneyMapIntegration.updateCapturePointWaypoints(points);
    }

    public List<Integer> getPointIds() { return pointIds; }
    public List<Double> getProgressPercentages() { return progressPercentages; }
    public List<Integer> getOwnerTeamOrdinals() { return ownerTeamOrdinals; }
    public List<String> getPointNames() { return pointNames; }
    public List<Integer> getCapturingTeamOrdinals() { return capturingTeamOrdinals; }
}
