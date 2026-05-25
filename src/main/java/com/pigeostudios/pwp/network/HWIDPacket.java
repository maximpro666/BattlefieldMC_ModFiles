package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.punishment.PunishmentManager;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Collections;
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
            if (player == null) return;

            String ip = "";
            try {
                Connection conn = player.connection.connection;
                if (conn.getRemoteAddress() instanceof InetSocketAddress addr) {
                    ip = addr.getAddress().getHostAddress();
                }
            } catch (Exception ignored) {}

            String serverHwid = generateServerHWID(player, ip);
            PWP.LOGGER.info("Server-side HWID for {}: {}", player.getName().getString(), serverHwid);

            if (PunishmentManager.isBannedByHWID(serverHwid)) {
                player.connection.disconnect(net.minecraft.network.chat.Component.literal(
                    "§cВаше оборудование заблокировано на этом сервере.\nHWID ban is active on this account."));
                return;
            }

            PunishmentManager.setHWID(player.getUUID(), serverHwid);
            PunishmentManager.setIP(player.getUUID(), ip);

            if (PunishmentManager.isBannedByIP(ip)) {
                player.connection.disconnect(net.minecraft.network.chat.Component.literal(
                    "§cВаш IP-адрес заблокирован на этом сервере."));
            }
        });
        return true;
    }

    public static String generateServerHWID(ServerPlayer player, String ip) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(ip).append("|");
            sb.append(player.getUUID()).append("|");
            var interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                for (var ni : Collections.list(interfaces)) {
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null && mac.length > 0) {
                        StringBuilder macStr = new StringBuilder();
                        for (byte b : mac) macStr.append(String.format("%02X", b));
                        sb.append(macStr);
                        break;
                    }
                }
            }
            sb.append("|").append(System.getProperty("user.name", "unknown"));
            String raw = sb.toString();
            return String.format("%08x", raw.hashCode());
        } catch (Exception e) {
            return String.format("%08x", (ip + player.getUUID()).hashCode());
        }
    }
}
