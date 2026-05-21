package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
            if (player == null) return;

            if (!RateLimiter.checkAndThrottle(player)) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.checkStringLength(vehicleId, 128))) return;
            if (vehicleId.isEmpty()) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requirePlaying(PWP.getGameManager()))) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireTeamPlayable(player))) return;

            Component result = PWP.getVehicleManager().buyVehicle(player, vehicleId, PWP.getTeamManager());
            if (result != null) {
                player.displayClientMessage(result, false);
            }
        });
        return true;
    }
}
