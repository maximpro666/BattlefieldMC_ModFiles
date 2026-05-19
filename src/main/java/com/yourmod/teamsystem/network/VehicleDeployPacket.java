package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.yourmod.teamsystem.core.ChatHelper.*;

public class VehicleDeployPacket {
    private final String vehicleId;

    public VehicleDeployPacket(String vehicleId) {
        this.vehicleId = vehicleId != null ? vehicleId : "";
    }

    public VehicleDeployPacket(FriendlyByteBuf buf) {
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
            if (!PacketValidator.checkAndReject(player, PacketValidator.requirePlaying(TeamSystem.getGameManager()))) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireTeamPlayable(player))) return;

            String result = TeamSystem.getVehicleManager().buyVehicle(player, vehicleId, TeamSystem.getTeamManager());
            if (result != null) {
                player.displayClientMessage(error(result), false);
            }
        });
        return true;
    }
}
