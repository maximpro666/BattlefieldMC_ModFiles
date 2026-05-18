package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KitAdminConfigSyncPacket {
    private final String configJson;

    public KitAdminConfigSyncPacket(String configJson) {
        this.configJson = configJson != null ? configJson : "{}";
    }

    public KitAdminConfigSyncPacket(FriendlyByteBuf buf) {
        this.configJson = buf.readUtf(65535);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(configJson);
    }

    public String getConfigJson() {
        return configJson;
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.yourmod.teamsystem.client.ClientTeamData.kitConfigEditJson = configJson;
            });
        });
        return true;
    }
}
