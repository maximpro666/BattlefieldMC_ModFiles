package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RespawnBeaconSyncPacket {

    public record BeaconData(String name, double x, double y, double z, int teamOrdinal) {}

    private final List<BeaconData> beacons;

    public RespawnBeaconSyncPacket(List<BeaconData> beacons) {
        this.beacons = beacons != null ? beacons : new ArrayList<>();
    }

    public RespawnBeaconSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.beacons = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String name       = buf.readUtf(128);
            double x          = buf.readDouble();
            double y          = buf.readDouble();
            double z          = buf.readDouble();
            int    teamOrdinal = buf.readInt();
            beacons.add(new BeaconData(name, x, y, z, teamOrdinal));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(beacons.size());
        for (BeaconData b : beacons) {
            buf.writeUtf(b.name(), 128);
            buf.writeDouble(b.x());
            buf.writeDouble(b.y());
            buf.writeDouble(b.z());
            buf.writeInt(b.teamOrdinal());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                List<ClientTeamData.ClientBeaconData> list = new ArrayList<>();
                for (BeaconData b : beacons) {
                    list.add(new ClientTeamData.ClientBeaconData(b.name(), b.x(), b.y(), b.z(), b.teamOrdinal()));
                }
                ClientTeamData.beacons = list;
                com.yourmod.teamsystem.client.journeymap.JourneyMapIntegration.updateBeaconWaypoints(list);
            })
        );
        context.setPacketHandled(true);
        return true;
    }

    public List<BeaconData> getBeacons() {
        return beacons;
    }
}
