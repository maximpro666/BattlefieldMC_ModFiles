package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.punishment.PunishmentManager;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

public class HWIDPacket {
    private final String hwid;

    public HWIDPacket(String hwid) {
        this.hwid = hwid != null ? hwid : "";
    }

    public HWIDPacket(FriendlyByteBuf buf) {
        this.hwid = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(hwid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || hwid.isEmpty()) return;

            PWP.LOGGER.info("HWID received from {}: {}", player.getName().getString(), hwid);

            if (PunishmentManager.isBannedByHWID(hwid)) {
                player.connection.disconnect(net.minecraft.network.chat.Component.literal(
                    "§cВаше оборудование заблокировано на этом сервере.\nHWID ban is active on this account."));
                return;
            }

            String ip = "";
            try {
                Connection conn = player.connection.connection;
                if (conn.getRemoteAddress() instanceof InetSocketAddress addr) {
                    ip = addr.getAddress().getHostAddress();
                }
            } catch (Exception ignored) {}

            PunishmentManager.setHWID(player.getUUID(), hwid);
            PunishmentManager.setIP(player.getUUID(), ip);

            if (PunishmentManager.isBannedByIP(ip)) {
                player.connection.disconnect(net.minecraft.network.chat.Component.literal(
                    "§cВаш IP-адрес заблокирован на этом сервере."));
            }
        });
        return true;
    }
}
