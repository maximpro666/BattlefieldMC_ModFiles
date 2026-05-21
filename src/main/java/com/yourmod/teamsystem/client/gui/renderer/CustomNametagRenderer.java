package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.UUID;

public class CustomNametagRenderer {

    private static final int MAX_DIST = 5;
    private static final int PADDING = 3;

    public void renderNametag(PoseStack poseStack, MultiBufferSource bufferSource,
                                Player player, Camera camera) {
        if (player == Minecraft.getInstance().player) return;
        UUID uuid = player.getUUID();

        Team myTeam = ClientTeamData.getLocalPlayerTeam();
        if (myTeam == Team.SPECTATOR) return;

        Map<UUID, PlayerListEntry> map = ClientTeamData.playerDataMap;
        if (map == null) return;
        PlayerListEntry ple = map.get(uuid);
        if (ple == null) return;

        if (ple.teamOrdinal() != myTeam.ordinal()) return;

        String squadName = ple.squadName() != null && !ple.squadName().isEmpty() ? ple.squadName() : null;
        if (squadName == null) return;

        Vec3 camPos = camera.getPosition();
        double dx = player.getX() - camPos.x;
        double dy = player.getY() + player.getBbHeight() + 0.8 - camPos.y;
        double dz = player.getZ() - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > MAX_DIST) return;

        float alpha = calcAlpha(dist);
        if (alpha < 0.02f) return;

        Font font = Minecraft.getInstance().font;

        String label = "\u2694 " + squadName;
        int textW = font.width(label);
        int panelW = textW + PADDING * 2;
        int panelH = font.lineHeight + PADDING * 2;

        float scale = 0.02f;
        int panelX = -(panelW / 2);
        int panelY = -panelH;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-scale, -scale, scale);

        int bgColor = ((int)(0xAA * alpha) << 24) | 0x0A0A0A;
        drawRect(poseStack, bufferSource, panelX, panelY, panelX + panelW, panelY + panelH, bgColor);

        int borderColor = ((int)(0x44 * alpha) << 24) | 0x808080;
        drawRect(poseStack, bufferSource, panelX, panelY, panelX + panelW, panelY + 1, borderColor);

        int textAlpha = (int)(0xFF * alpha);
        int textColor = (textAlpha << 24) | 0xFFFFFF;

        font.drawInBatch(label, panelX + PADDING, panelY + PADDING, textColor, true,
            poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

        poseStack.popPose();
    }

    private static float calcAlpha(double dist) {
        if (dist <= 3) return 1.0f;
        if (dist >= MAX_DIST) return 0.0f;
        return (float)(1.0 - (dist - 3) / (MAX_DIST - 3));
    }

    private void drawRect(PoseStack poseStack, MultiBufferSource bufferSource,
                           float x1, float y1, float x2, float y2, int color) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.gui());
        Matrix4f mat = poseStack.last().pose();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        vc.vertex(mat, x1, y2, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x2, y2, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x2, y1, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x1, y1, 0).color(r, g, b, a).endVertex();
    }
}
