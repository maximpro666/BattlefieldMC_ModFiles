package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleSpawnPacket {
    private final String vehicleId;

    public VehicleSpawnPacket(String vehicleId) {
        this.vehicleId = vehicleId != null ? vehicleId : "";
    }

    public VehicleSpawnPacket(FriendlyByteBuf buf) {
        this.vehicleId = buf.readUtf(128);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(vehicleId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || vehicleId.isEmpty()) return;
            String result = TeamSystem.getVehicleManager().buyVehicle(player, vehicleId, TeamSystem.getTeamManager());
            if (result != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(result), false);
            }
        });
        return true;
    }
}
