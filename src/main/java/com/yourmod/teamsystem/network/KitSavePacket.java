package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KitSavePacket {
    private final String kitId;
    private final String loadoutConfig;

    public KitSavePacket(String kitId, String loadoutConfig) {
        this.kitId = kitId != null ? kitId : "";
        this.loadoutConfig = loadoutConfig != null ? loadoutConfig : "";
    }

    public KitSavePacket(FriendlyByteBuf buf) {
        this.kitId = buf.readUtf(128);
        this.loadoutConfig = buf.readUtf(32767);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(kitId);
        buf.writeUtf(loadoutConfig);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || kitId.isEmpty()) return;
            var data = TeamSystem.getTeamManager().getOrCreatePlayerData(player.getUUID());
            if (data != null) {
                data.setLoadoutConfig(loadoutConfig);
                TeamSystem.getTeamManager().setDirty();
            }
        });
        return true;
    }
}
