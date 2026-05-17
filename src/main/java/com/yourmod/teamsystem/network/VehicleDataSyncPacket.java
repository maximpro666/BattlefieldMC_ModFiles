package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.VehicleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class VehicleDataSyncPacket {
    private final List<VehicleData> vehicles;

    public VehicleDataSyncPacket(List<VehicleData> vehicles) {
        this.vehicles = vehicles != null ? vehicles : new ArrayList<>();
    }

    public VehicleDataSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.vehicles = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String vehicleId = buf.readUtf(128);
            String displayName = buf.readUtf(256);
            String description = buf.readUtf(512);
            String icon = buf.readUtf(64);
            int ticketCost = buf.readInt();
            int minRank = buf.readInt();
            int cooldown = buf.readInt();
            boolean available = buf.readBoolean();
            vehicles.add(new VehicleData(vehicleId, displayName, description, icon, ticketCost, minRank, cooldown, available));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(vehicles.size());
        for (VehicleData v : vehicles) {
            buf.writeUtf(v.vehicleId());
            buf.writeUtf(v.displayName());
            buf.writeUtf(v.description());
            buf.writeUtf(v.icon());
            buf.writeInt(v.ticketCost());
            buf.writeInt(v.minRank());
            buf.writeInt(v.cooldown());
            buf.writeBoolean(v.available());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTeamData.vehicles = vehicles;
            });
        });
        return true;
    }
}
