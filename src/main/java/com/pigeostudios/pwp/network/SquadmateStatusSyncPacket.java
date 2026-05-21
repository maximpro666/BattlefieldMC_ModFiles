package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class SquadmateStatusSyncPacket {

    public record SyncEntry(UUID uuid, int cooldownTicks) {}

    private final List<SyncEntry> entries;

    public SquadmateStatusSyncPacket(List<SyncEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }

    public SquadmateStatusSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid          = buf.readUUID();
            int  cooldownTicks = buf.readInt();
            entries.add(new SyncEntry(uuid, cooldownTicks));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entries.size());
        for (SyncEntry e : entries) {
            buf.writeUUID(e.uuid());
            buf.writeInt(e.cooldownTicks());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Map<UUID, Integer> map = new HashMap<>();
                for (SyncEntry e : entries) {
                    map.put(e.uuid(), e.cooldownTicks());
                }
                ClientTeamData.squadmateStatuses = map;
            })
        );
        context.setPacketHandled(true);
        return true;
    }

    public List<SyncEntry> getEntries() {
        return entries;
    }
}
