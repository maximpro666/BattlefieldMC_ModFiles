package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.bleeding.BleedingHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ReviveActionPacket {
    public enum Action { START, CANCEL }

    private final Action action;
    private final UUID targetUUID;

    public ReviveActionPacket(Action action, UUID targetUUID) {
        this.action = action;
        this.targetUUID = targetUUID;
    }

    public ReviveActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.targetUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUUID(targetUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer helper = ctx.getSender();
            if (helper == null) return;

            ServerPlayer target = helper.getServer().getPlayerList().getPlayer(targetUUID);
            if (target == null) return;

            var state = BleedingHandler.getState(target);
            if (state == null || !state.isBleeding()) return;

            var tm = PWP.getTeamManager();
            if (tm == null || !tm.isFriendly(helper, target)) return;

            if (action == Action.START) {
                state.reviverUUID = helper.getUUID();
                state.reviveProgress = 0;
            } else {
                state.reviverUUID = null;
                state.reviveProgress = 0;
            }

            var bh = BleedingHandler.getInstance();
            if (bh != null) bh.syncToClient(target);
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
