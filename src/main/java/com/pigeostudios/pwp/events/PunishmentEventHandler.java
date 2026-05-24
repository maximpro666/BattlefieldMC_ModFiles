package com.pigeostudios.pwp.events;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.punishment.PunishmentManager;
import com.pigeostudios.pwp.report.ReportManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PunishmentEventHandler {

    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;

        if (PunishmentManager.isMuted(player.getUUID())) {
            event.setCanceled(true);
            String reason = PunishmentManager.getMuteReason(player.getUUID());
            player.sendSystemMessage(Component.literal("§cВы заглушены в чате.")
                .withStyle(ChatFormatting.RED));
            if (!reason.isEmpty()) {
                player.sendSystemMessage(Component.literal("§7Причина: " + reason)
                    .withStyle(ChatFormatting.GRAY));
            }
            PWP.LOGGER.info("Blocked chat from muted player {}: {}", player.getName().getString(), event.getMessage());
        }
    }

    public static void checkBanOnLogin(ServerPlayer player) {
        if (PunishmentManager.isBanned(player.getUUID())) {
            String reason = PunishmentManager.getBanReason(player.getUUID());
            player.connection.disconnect(Component.literal(
                "§cВы забанены на этом сервере.\n§7Причина: " + reason));
        }
    }

    public static void checkBanOnLoginHWID(ServerPlayer player, String hwid) {
        if (PunishmentManager.isBannedByHWID(hwid)) {
            player.connection.disconnect(Component.literal(
                "§cВаше оборудование заблокировано на этом сервере."));
        }
    }
}
