package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.KitData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class KitDataSyncPacket {
    private final List<KitData> kits;

    public KitDataSyncPacket(List<KitData> kits) {
        this.kits = kits != null ? kits : new ArrayList<>();
    }

    public KitDataSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.kits = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String name = buf.readUtf(128);
            String displayName = buf.readUtf(256);
            String description = buf.readUtf(512);
            String icon = buf.readUtf(64);
            int minRank = buf.readInt();
            int cooldown = buf.readInt();
            boolean available = buf.readBoolean();
            String loadoutJson = buf.readUtf(4096);
            kits.add(new KitData(name, displayName, description, icon, minRank, cooldown, available, loadoutJson));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(kits.size());
        for (KitData kit : kits) {
            buf.writeUtf(kit.name());
            buf.writeUtf(kit.displayName());
            buf.writeUtf(kit.description());
            buf.writeUtf(kit.icon());
            buf.writeInt(kit.minRank());
            buf.writeInt(kit.cooldown());
            buf.writeBoolean(kit.available());
            buf.writeUtf(kit.loadoutJson());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTeamData.kits = kits;
            });
        });
        return true;
    }
}
