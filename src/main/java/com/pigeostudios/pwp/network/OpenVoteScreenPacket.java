package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.screen.VoteScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenVoteScreenPacket {
    private final List<String> mapNames;
    private final int[] voteCounts;
    private final int remainingSeconds;

    public OpenVoteScreenPacket(List<String> mapNames, int[] voteCounts, int remainingSeconds) {
        this.mapNames = mapNames != null ? mapNames : new ArrayList<>();
        this.voteCounts = voteCounts != null ? voteCounts : new int[0];
        this.remainingSeconds = remainingSeconds;
    }

    public OpenVoteScreenPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.mapNames = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mapNames.add(buf.readUtf(128));
        }
        this.voteCounts = buf.readVarIntArray();
        this.remainingSeconds = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(mapNames.size());
        for (String name : mapNames) {
            buf.writeUtf(name);
        }
        buf.writeVarIntArray(voteCounts);
        buf.writeVarInt(remainingSeconds);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTeamData.setVoteData(mapNames, voteCounts, remainingSeconds);
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null && mc.player != null) {
                    VoteScreen.open(mapNames, voteCounts, remainingSeconds);
                }
            });
        });
        return true;
    }
}
