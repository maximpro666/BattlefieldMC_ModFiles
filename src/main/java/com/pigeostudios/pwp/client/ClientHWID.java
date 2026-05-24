package com.pigeostudios.pwp.client;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.network.HWIDPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class ClientHWID {

    public static String generateHWID() {
        try {
            StringBuilder sb = new StringBuilder();

            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface ni = nets.nextElement();
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null && mac.length == 6) {
                        StringBuilder macStr = new StringBuilder();
                        for (byte b : mac) {
                            macStr.append(String.format("%02X", b));
                        }
                        sb.append(macStr);
                        break;
                    }
                }
            } catch (Exception ignored) {}

            sb.append(System.getProperty("os.name", "unknown"));
            sb.append(System.getProperty("os.version", "unknown"));
            sb.append(System.getProperty("user.name", "unknown"));

            try {
                var fs = java.nio.file.FileSystems.getDefault();
                var stores = fs.getFileStores();
                for (var store : stores) {
                    sb.append(store.name());
                    sb.append(store.type());
                    break;
                }
            } catch (Exception ignored) {}

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to generate HWID", e);
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static boolean sent = false;

    public static void onPlayerJoin() {
        if (sent) return;
        sent = true;
        String hwid = generateHWID();
        PacketHandler.CHANNEL.sendToServer(new HWIDPacket(hwid));
    }

    public static void reset() {
        sent = false;
    }
}
