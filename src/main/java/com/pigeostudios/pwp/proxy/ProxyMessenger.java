package com.pigeostudios.pwp.proxy;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;

public class ProxyMessenger {
    private static final ResourceLocation CHANNEL = new ResourceLocation("pwp", "proxy");
    private static MinecraftServer server;

    public static void init(MinecraftServer srv) {
        server = srv;
    }

    public static void send(String message) {
        if (server == null) return;
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer player = players.get(0);
        if (player.connection == null) return;

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(msgBytes.length);
        buf.writeBytes(msgBytes);

        player.connection.send(new ClientboundCustomPayloadPacket(CHANNEL, buf));
    }

    public static boolean isMatchServer() {
        return "match".equals(System.getProperty("pwp.mode"));
    }
}
