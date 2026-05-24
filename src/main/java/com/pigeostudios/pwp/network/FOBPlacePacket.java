package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.pigeostudios.pwp.core.ChatHelper.*;

public class FOBPlacePacket {
    private final String name;

    public FOBPlacePacket(String name) {
        this.name = name != null ? name : "FOB";
    }

    public FOBPlacePacket(FriendlyByteBuf buf) {
        this.name = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(name);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!RateLimiter.checkAndThrottle(player)) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.checkStringLength(name, 64))) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requirePlaying(PWP.getGameManager()))) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireTeamPlayable(player))) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireSquadLeader(player))) return;

            String result = PWP.getServiceRegistry().getFOB().placeFOB(player, name);
            if (result != null) {
                player.displayClientMessage(error(result), false);
            }
        });
        return true;
    }
}
