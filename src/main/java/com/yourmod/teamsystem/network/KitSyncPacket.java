package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class KitSyncPacket {
    private List<String> kitNames;
    private List<String> kitDisplayNames;
    private List<Integer> minRankOrdinals;

    public KitSyncPacket(List<String> kitNames, List<String> kitDisplayNames, List<Integer> minRankOrdinals) {
        this.kitNames = kitNames;
        this.kitDisplayNames = kitDisplayNames;
        this.minRankOrdinals = minRankOrdinals;
    }

    public KitSyncPacket(FriendlyByteBuf buf) {
        int kitCount = buf.readInt();
        this.kitNames = new ArrayList<>();
        this.kitDisplayNames = new ArrayList<>();
        this.minRankOrdinals = new ArrayList<>();

        for (int i = 0; i < kitCount; i++) {
            kitNames.add(buf.readUtf(256));
            kitDisplayNames.add(buf.readUtf(256));
            minRankOrdinals.add(buf.readInt());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(kitNames.size());
        for (int i = 0; i < kitNames.size(); i++) {
            buf.writeUtf(kitNames.get(i));
            buf.writeUtf(kitDisplayNames.get(i));
            buf.writeInt(minRankOrdinals.get(i));
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                handleClientSide(kitNames, kitDisplayNames, minRankOrdinals));
        });
        return true;
    }

    private static void handleClientSide(List<String> kitNames, List<String> kitDisplayNames, List<Integer> minRankOrdinals) {
        // Client-side kit update
    }

    public List<String> getKitNames() { return kitNames; }
    public List<String> getKitDisplayNames() { return kitDisplayNames; }
    public List<Integer> getMinRankOrdinals() { return minRankOrdinals; }
}
