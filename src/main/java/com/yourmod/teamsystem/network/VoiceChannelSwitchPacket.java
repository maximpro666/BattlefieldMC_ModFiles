package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.core.TeamVoicePlugin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VoiceChannelSwitchPacket {

    private final int channel;

    public VoiceChannelSwitchPacket(int channel) {
        this.channel = channel;
    }

    public static void encode(VoiceChannelSwitchPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.channel);
    }

    public static VoiceChannelSwitchPacket decode(FriendlyByteBuf buf) {
        return new VoiceChannelSwitchPacket(buf.readInt());
    }

    public static void handle(VoiceChannelSwitchPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var gm = TeamVoicePlugin.getGroupManager();
            if (gm != null) {
                gm.joinChannel(player, msg.channel);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
