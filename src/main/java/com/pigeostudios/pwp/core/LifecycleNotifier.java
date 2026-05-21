package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.network.NotificationPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LifecycleNotifier {
    private static final Map<String, Map<String, String>> MESSAGES = new LinkedHashMap<>();

    static {
        Map<String, String> en = new LinkedHashMap<>();
        en.put("match_cleaning", "\u00a76[PWP] \u00a7eCleaning up old match server...");
        en.put("match_waiting", "\u00a76[PWP] \u00a7eWaiting for match server to start...");
        en.put("match_ready", "\u00a76[PWP] \u00a7aMatch server ready! Transferring players...");
        en.put("match_failed", "\u00a76[PWP] \u00a7cMatch server failed to start in time");
        en.put("match_ending", "\u00a76[PWP] \u00a7eMatch ending, returning to lobby...");
        en.put("match_10min", "\u00a7e\u26a0 \u00a7f10 minutes remaining");
        en.put("match_5min", "\u00a7e\u26a0 \u00a7f5 minutes remaining");
        en.put("match_2min", "\u00a7c\u26a0 \u00a7f2 minutes remaining");
        en.put("match_1min", "\u00a7c\u26a0 \u00a7f1 minute remaining");
        en.put("match_30s", "\u00a7c\u26a0 \u00a7f30 seconds remaining");
        en.put("match_10s", "\u00a7c\u26a0 \u00a7f10 seconds remaining");
        en.put("match_overtime", "\u00a7c\u26a0 \u00a7lOVERTIME!");
        en.put("match_tickets_low", "\u00a7c\u26a0 \u00a7fTickets low!");
        en.put("returning_lobby", "\u00a76[PWP] \u00a7eReturning to lobby...");
        en.put("cycle_detected", "\u00a76[PWP] \u00a7eMatch cycle detected, starting next match...");
        en.put("auto_start", "\u00a76[PWP] \u00a7eStarting new match...");

        Map<String, String> ru = new LinkedHashMap<>();
        ru.put("match_cleaning", "\u00a76[PWP] \u00a7e\u041e\u0447\u0438\u0441\u0442\u043a\u0430 \u0441\u0442\u0430\u0440\u043e\u0433\u043e \u043c\u0430\u0442\u0447-\u0441\u0435\u0440\u0432\u0435\u0440\u0430...");
        ru.put("match_waiting", "\u00a76[PWP] \u00a7e\u041e\u0436\u0438\u0434\u0430\u043d\u0438\u0435 \u0437\u0430\u043f\u0443\u0441\u043a\u0430 \u043c\u0430\u0442\u0447-\u0441\u0435\u0440\u0432\u0435\u0440\u0430...");
        ru.put("match_ready", "\u00a76[PWP] \u00a7a\u041c\u0430\u0442\u0447-\u0441\u0435\u0440\u0432\u0435\u0440 \u0433\u043e\u0442\u043e\u0432! \u041f\u0435\u0440\u0435\u043c\u0435\u0449\u0435\u043d\u0438\u0435 \u0438\u0433\u0440\u043e\u043a\u043e\u0432...");
        ru.put("match_failed", "\u00a76[PWP] \u00a7c\u041c\u0430\u0442\u0447-\u0441\u0435\u0440\u0432\u0435\u0440 \u043d\u0435 \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u043b\u0441\u044f \u0432\u043e\u0432\u0440\u0435\u043c\u044f");
        ru.put("match_ending", "\u00a76[PWP] \u00a7e\u041c\u0430\u0442\u0447 \u0437\u0430\u0432\u0435\u0440\u0448\u0430\u0435\u0442\u0441\u044f, \u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u0432 \u043b\u043e\u0431\u0431\u0438...");
        ru.put("match_10min", "\u00a7e\u26a0 \u00a7f\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c 10 \u043c\u0438\u043d\u0443\u0442");
        ru.put("match_5min", "\u00a7e\u26a0 \u00a7f\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c 5 \u043c\u0438\u043d\u0443\u0442");
        ru.put("match_2min", "\u00a7c\u26a0 \u00a7f\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c 2 \u043c\u0438\u043d\u0443\u0442\u044b");
        ru.put("match_1min", "\u00a7c\u26a0 \u00a7f\u041e\u0441\u0442\u0430\u043b\u0430\u0441\u044c 1 \u043c\u0438\u043d\u0443\u0442\u0430");
        ru.put("match_30s", "\u00a7c\u26a0 \u00a7f\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c 30 \u0441\u0435\u043a\u0443\u043d\u0434");
        ru.put("match_10s", "\u00a7c\u26a0 \u00a7f\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c 10 \u0441\u0435\u043a\u0443\u043d\u0434");
        ru.put("match_overtime", "\u00a7c\u26a0 \u00a7l\u041e\u0412\u0415\u0420\u0422\u0410\u0419\u041c!");
        ru.put("match_tickets_low", "\u00a7c\u26a0 \u00a7f\u041c\u0430\u043b\u043e \u0442\u0438\u043a\u0435\u0442\u043e\u0432!");
        ru.put("returning_lobby", "\u00a76[PWP] \u00a7e\u0412\u043e\u0437\u0432\u0440\u0430\u0449\u0435\u043d\u0438\u0435 \u0432 \u043b\u043e\u0431\u0431\u0438...");
        ru.put("cycle_detected", "\u00a76[PWP] \u00a7e\u041e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d \u0446\u0438\u043a\u043b \u043c\u0430\u0442\u0447\u0430, \u0437\u0430\u043f\u0443\u0441\u043a \u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0435\u0433\u043e...");
        ru.put("auto_start", "\u00a76[PWP] \u00a7e\u0417\u0430\u043f\u0443\u0441\u043a \u043d\u043e\u0432\u043e\u0433\u043e \u043c\u0430\u0442\u0447\u0430...");

        MESSAGES.put("en", en);
        MESSAGES.put("ru", ru);
    }

    private LifecycleNotifier() {}

    private static String getText(String key) {
        String lang = PWP.getConfig() != null ? PWP.getConfig().getLanguage() : "en";
        Map<String, String> map = MESSAGES.getOrDefault(lang, MESSAGES.get("en"));
        return map.getOrDefault(key, key);
    }

    public static void broadcastToAll(MinecraftServer server, String key) {
        if (server == null) return;
        Component msg = Component.literal(getText(key));
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    public static void sendToPlayer(ServerPlayer player, String key) {
        if (player == null) return;
        player.sendSystemMessage(Component.literal(getText(key)));
    }

    public static void sendNotification(ServerPlayer player, String key, int durationMs) {
        if (player == null) return;
        String text = getText(key);
        NotificationPacket pkt = new NotificationPacket(text, "lifecycle", durationMs, "");
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pkt);
    }

    public static void broadcastNotification(MinecraftServer server, String key, int durationMs) {
        if (server == null) return;
        String text = getText(key);
        NotificationPacket pkt = new NotificationPacket(text, "lifecycle", durationMs, "");
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
    }
}
