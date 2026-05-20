package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.data.KitConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.function.Supplier;

public class KitConfigSyncPacket {
    private final String configJson;

    public KitConfigSyncPacket(String configJson) {
        this.configJson = configJson != null ? configJson : "";
    }

    public KitConfigSyncPacket(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        byte[] compressed = new byte[len];
        buf.readBytes(compressed);
        String json;
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            json = new String(gzip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            json = "";
            e.printStackTrace();
        }
        this.configJson = json;
    }

    public void toBytes(FriendlyByteBuf buf) {
        try {
            byte[] input = configJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(input);
            }
            byte[] compressed = baos.toByteArray();
            buf.writeVarInt(compressed.length);
            buf.writeBytes(compressed);
        } catch (Exception e) {
            e.printStackTrace();
            buf.writeVarInt(0);
        }
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
