package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FOBPlacePacket {
    private final String name;

    public FOBPlacePacket(String name) {
        this.name = name != null ? name : "FOB";
    }

    public FOBPlacePacket(FriendlyByteBuf buf) {
        this.name = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(name);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            String result = TeamSystem.getFOBManager().placeFOB(player, name);
            if (result != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(result), false);
            }
        });
        return true;
    }
}
