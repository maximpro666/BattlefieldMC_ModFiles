package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.FOBData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FOBSyncPacket {
    private final List<FOBData> fobs;

    public FOBSyncPacket(List<FOBData> fobs) {
        this.fobs = fobs != null ? fobs : new ArrayList<>();
    }

    public FOBSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.fobs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int fobId = buf.readInt();
            String name = buf.readUtf(128);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            String worldKey = buf.readUtf(64);
            int teamOrdinal = buf.readInt();
            float health = buf.readFloat();
            fobs.add(new FOBData(fobId, name, x, y, z, worldKey, teamOrdinal, health));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(fobs.size());
        for (FOBData fob : fobs) {
            buf.writeInt(fob.fobId());
            buf.writeUtf(fob.name());
            buf.writeDouble(fob.x());
            buf.writeDouble(fob.y());
            buf.writeDouble(fob.z());
            buf.writeUtf(fob.worldKey());
            buf.writeInt(fob.teamOrdinal());
            buf.writeFloat(fob.health());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTeamData.fobs = fobs;
                com.yourmod.teamsystem.client.journeymap.JourneyMapIntegration.updateFOBWaypoints(fobs);
            });
        });
        return true;
    }
}
