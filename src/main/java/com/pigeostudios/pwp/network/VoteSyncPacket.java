package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.screen.VoteScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VoteSyncPacket {
    private final int remainingSeconds;
    private final int[] voteCounts;

    public VoteSyncPacket(int remainingSeconds, int[] voteCounts) {
        this.remainingSeconds = remainingSeconds;
        this.voteCounts = voteCounts != null ? voteCounts : new int[0];
    }

    public VoteSyncPacket(FriendlyByteBuf buf) {
        this.remainingSeconds = buf.readVarInt();
        this.voteCounts = buf.readVarIntArray();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(remainingSeconds);
        buf.writeVarIntArray(voteCounts);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTeamData.updateVoteCounts(remainingSeconds, voteCounts);
                Screen s = Minecraft.getInstance().screen;
                if (s instanceof VoteScreen vs) {
                    vs.updateVotes(remainingSeconds, voteCounts);
                }
            });
        });
        return true;
    }
}
