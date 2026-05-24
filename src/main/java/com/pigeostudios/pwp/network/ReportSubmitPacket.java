package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.report.*;
import com.pigeostudios.pwp.core.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ReportSubmitPacket {
    private final String targetName;
    private final String type;
    private final String description;

    public ReportSubmitPacket(String targetName, String type, String description) {
        this.targetName = targetName != null ? targetName : "";
        this.type = type != null ? type : "";
        this.description = description != null ? description : "";
    }

    public ReportSubmitPacket(FriendlyByteBuf buf) {
        this.targetName = buf.readUtf(64);
        this.type = buf.readUtf(64);
        this.description = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(targetName);
        buf.writeUtf(type);
        buf.writeUtf(description);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || targetName.isEmpty() || type.isEmpty()) return;

            ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(targetName);
            if (target == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cPlayer not found"));
                return;
            }

            if (target == player) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cCannot report yourself"));
                return;
            }

            ReportType reportType;
            try {
                reportType = ReportType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cUnknown report type"));
                return;
            }

            int id = ReportManager.createReport(player.getUUID(), target.getUUID(),
                target.getName().getString(), reportType, description);

            ReportManager.addSystemMessage(id, "§eTicket #" + id + " created by §f" + player.getName().getString()
                + "§e for §f" + target.getName().getString() + " §e(" + reportType.getDisplayName() + ")");

            ReportManager.notifyStaff(player.getServer(), id,
                player.getName().getString(), target.getName().getString(), reportType);

            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§aReport #" + id + " has been sent!"));
        });
        return true;
    }
}
