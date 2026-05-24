package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class BleedingSyncPacket {
    private final UUID playerUUID;
    private final boolean bleeding;
    private final int bleedTimeRemaining;
    private final UUID reviverUUID;
    private final int reviveProgress;

    public BleedingSyncPacket(UUID playerUUID, boolean bleeding, int bleedTimeRemaining,
                              UUID reviverUUID, int reviveProgress) {
        this.playerUUID = playerUUID;
        this.bleeding = bleeding;
        this.bleedTimeRemaining = bleedTimeRemaining;
        this.reviverUUID = reviverUUID;
        this.reviveProgress = reviveProgress;
    }

    public BleedingSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.bleeding = buf.readBoolean();
        this.bleedTimeRemaining = buf.readInt();
        this.reviverUUID = buf.readBoolean() ? buf.readUUID() : null;
        this.reviveProgress = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(bleeding);
        buf.writeInt(bleedTimeRemaining);
        buf.writeBoolean(reviverUUID != null);
        if (reviverUUID != null) buf.writeUUID(reviverUUID);
        buf.writeInt(reviveProgress);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            if (!mc.player.getUUID().equals(playerUUID)) return;

            ClientTeamData.isBleeding = bleeding;
            ClientTeamData.bleedTimeRemaining = bleedTimeRemaining;
            ClientTeamData.reviverUUID = reviverUUID;
            ClientTeamData.reviveProgress = reviveProgress;
        });
        supplier.get().setPacketHandled(true);
        return true;
    }
}
