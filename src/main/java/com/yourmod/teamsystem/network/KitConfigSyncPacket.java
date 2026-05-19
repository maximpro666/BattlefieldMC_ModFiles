package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.data.KitConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KitConfigSyncPacket {
    private final String configJson;

    public KitConfigSyncPacket(String configJson) {
        this.configJson = configJson != null ? configJson : "";
    }

    public KitConfigSyncPacket(FriendlyByteBuf buf) {
        this.configJson = buf.readUtf(65535);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(configJson);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTeamData.receivedKitConfigJson = configJson;
                if (configJson != null && !configJson.isEmpty()) {
                    try {
                        KitConfig cfg = KitConfig.GSON.fromJson(configJson, KitConfig.class);
                        if (cfg != null) {
                            KitConfig.set(cfg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        return true;
    }
}
