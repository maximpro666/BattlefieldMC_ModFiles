package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientVoiceHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class VoiceSpeakingStatePacket {
    private final UUID playerUUID;
    private final String playerName;
    private final int channel;

    public VoiceSpeakingStatePacket(UUID playerUUID, String playerName, int channel) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.channel = channel;
    }

    public VoiceSpeakingStatePacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.playerName = buf.readUtf(64);
        this.channel = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeUtf(playerName);
        buf.writeInt(channel);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientVoiceHandler.onPlayerSpeaking(playerUUID, playerName, channel);
            });
        });
        return true;
    }
}
