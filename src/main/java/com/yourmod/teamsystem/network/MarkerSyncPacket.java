package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientMarkerData;
import com.yourmod.teamsystem.core.MarkerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class MarkerSyncPacket {
    private final List<MarkerData> markers;

    public MarkerSyncPacket(List<MarkerData> markers) {
        this.markers = markers;
    }

    public static void encode(MarkerSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.markers.size());
        for (MarkerData m : msg.markers) {
            buf.writeUtf(m.getName());
            buf.writeUtf(m.getLabel());
            buf.writeUtf(m.getDimension().toString());
            buf.writeDouble(m.getX());
            buf.writeDouble(m.getY());
            buf.writeDouble(m.getZ());
            buf.writeInt(m.getTeamOrdinal());
            buf.writeInt(m.getType().ordinal());
            buf.writeUUID(m.getCreatorUUID());
            buf.writeLong(m.getExpiryTime());
            buf.writeBoolean(m.isPing());
        }
    }

    public static MarkerSyncPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<MarkerData> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String name = buf.readUtf();
            String label = buf.readUtf();
            ResourceLocation dim = new ResourceLocation(buf.readUtf());
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            int teamOrdinal = buf.readInt();
            int typeOrdinal = buf.readInt();
            UUID creator = buf.readUUID();
            MarkerData marker = new MarkerData(name, label, dim, x, y, z, teamOrdinal, MarkerData.MarkerType.values()[typeOrdinal], creator);
            marker.setExpiryTime(buf.readLong());
            marker.setPing(buf.readBoolean());
            list.add(marker);
        }
        return new MarkerSyncPacket(list);
    }

    public static void handle(MarkerSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientMarkerData.setMarkers(msg.markers));
        ctx.get().setPacketHandled(true);
    }
}
