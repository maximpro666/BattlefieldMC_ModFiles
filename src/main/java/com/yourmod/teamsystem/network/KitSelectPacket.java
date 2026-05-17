package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KitSelectPacket {
    private final String kitName;

    public KitSelectPacket(String kitName) {
        this.kitName = kitName != null ? kitName : "";
    }

    public KitSelectPacket(FriendlyByteBuf buf) {
        this.kitName = buf.readUtf(128);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(kitName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || kitName.isEmpty()) return;
            String result = TeamSystem.getKitManager().claimKit(player, kitName, TeamSystem.getTeamManager());
            if (result != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(result), false);
            }
        });
        return true;
    }
}
