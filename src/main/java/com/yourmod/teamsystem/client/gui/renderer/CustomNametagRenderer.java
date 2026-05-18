package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.core.Rank;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

public class CustomNametagRenderer {

    public void renderNametag(PoseStack poseStack, MultiBufferSource bufferSource,
                               Player player, Camera camera) {
        if (player == Minecraft.getInstance().player) return;
        UUID uuid = player.getUUID();

        Map<UUID, PlayerListEntry> map = ClientTeamData.playerDataMap;
        if (map == null) return;
        PlayerListEntry ple = map.get(uuid);
        if (ple == null) return;

        Rank rank = Rank.fromOrdinal(ple.rank());
        boolean russian = ple.teamOrdinal() == Team.RUSSIA.ordinal();
        String rankPrefix  = rank != null ? rank.getPrefix(russian) : "";
        String callsign    = ple.callsign() != null ? ple.callsign() : player.getName().getString();
        String displayText = "[" + rankPrefix + "] " + callsign;

        Vec3 camPos = camera.getPosition();
        double dx = player.getX() - camPos.x;
        double dy = player.getY() + player.getBbHeight() + 0.5 - camPos.y;
        double dz = player.getZ() - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 32) return;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);

        Minecraft mc = Minecraft.getInstance();
        int tw = mc.font.width(displayText);

        mc.font.draw(poseStack, displayText, -tw / 2f, 0f, 0xFFFFFFFF);

        if (!rankPrefix.isEmpty()) {
            String rankPart = "[" + rankPrefix + "]";
            mc.font.draw(poseStack, rankPart, -tw / 2f, 0f, 0xFFE07B00);
        }

        poseStack.popPose();
    }
}
