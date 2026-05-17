package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GameStateSyncPacket {
    private final int phaseOrdinal;
    private final String currentMapName;
    private final int winningTeamOrdinal;

    public GameStateSyncPacket(int phaseOrdinal, String currentMapName, int winningTeamOrdinal) {
        this.phaseOrdinal = phaseOrdinal;
        this.currentMapName = currentMapName != null ? currentMapName : "";
        this.winningTeamOrdinal = winningTeamOrdinal;
    }

    public static void encode(GameStateSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.phaseOrdinal);
        buf.writeUtf(msg.currentMapName);
        buf.writeInt(msg.winningTeamOrdinal);
    }

    public static GameStateSyncPacket decode(FriendlyByteBuf buf) {
        return new GameStateSyncPacket(
            buf.readInt(),
            buf.readUtf(),
            buf.readInt()
        );
    }

    public static void handle(GameStateSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientTeamData.setGamePhase(msg.phaseOrdinal);
            ClientTeamData.setCurrentMapName(msg.currentMapName);
            ClientTeamData.setWinningTeamOrdinal(msg.winningTeamOrdinal);
        });
        ctx.get().setPacketHandled(true);
    }
}
