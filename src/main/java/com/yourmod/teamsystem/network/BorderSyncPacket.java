package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class BorderSyncPacket {
    private List<byte[]> zoneTypes;
    private List<double[]> zoneData;

    public BorderSyncPacket(List<byte[]> zoneTypes, List<double[]> zoneData) {
        this.zoneTypes = zoneTypes;
        this.zoneData = zoneData;
    }

    public BorderSyncPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        zoneTypes = new ArrayList<>();
        zoneData = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            byte type = buf.readByte();
            zoneTypes.add(new byte[]{type});
            if (type == 0) {
                zoneData.add(new double[]{buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble()});
            } else {
                int verts = buf.readInt();
                double[] data = new double[verts * 2];
                for (int j = 0; j < verts; j++) {
                    data[j * 2] = buf.readDouble();
                    data[j * 2 + 1] = buf.readDouble();
                }
                zoneData.add(data);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(zoneTypes.size());
        for (int i = 0; i < zoneTypes.size(); i++) {
            byte type = zoneTypes.get(i)[0];
            buf.writeByte(type);
            double[] data = zoneData.get(i);
            if (type == 0) {
                buf.writeDouble(data[0]);
                buf.writeDouble(data[1]);
                buf.writeDouble(data[2]);
                buf.writeDouble(data[3]);
            } else {
                buf.writeInt(data.length / 2);
                for (double v : data) buf.writeDouble(v);
            }
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTeamData.borderZoneTypes = zoneTypes;
                ClientTeamData.borderZoneData = zoneData;
            });
        });
        return true;
    }
}
