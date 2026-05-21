package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConfigSyncPacket {
    private final String language;

    public ConfigSyncPacket(String language) {
        this.language = language != null ? language : "ru";
    }

    public ConfigSyncPacket(FriendlyByteBuf buf) {
        this.language = buf.readUtf(16);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(language);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTeamData.language = language;
            });
        });
        return true;
    }
}
