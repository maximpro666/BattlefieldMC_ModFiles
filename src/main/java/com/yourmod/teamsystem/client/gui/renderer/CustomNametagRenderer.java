package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.scoreboard.data.RankDefinition;
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

    private static final int MAX_DIST = 48;
    private static final int ICON_SIZE = 12;
    private static final int PADDING = 4;
    private static final int LINE_GAP = 1;

    public void renderNametag(PoseStack poseStack, MultiBufferSource bufferSource,
                                Player player, Camera camera) {
        if (player == Minecraft.getInstance().player) return;
        UUID uuid = player.getUUID();

        Map<UUID, PlayerListEntry> map = ClientTeamData.playerDataMap;
        if (map == null) return;
        PlayerListEntry ple = map.get(uuid);
        if (ple == null) return;

        Vec3 camPos = camera.getPosition();
        double dx = player.getX() - camPos.x;
        double dy = player.getY() + player.getBbHeight() + 0.5 - camPos.y;
        double dz = player.getZ() - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > MAX_DIST) return;

        float alpha = calcAlpha(dist);
        if (alpha < 0.02f) return;

        RankDefinition rankDef = RankDefinition.get(ple.rank());
        String callsign = ple.callsign() != null && !ple.callsign().isEmpty() ? ple.callsign() : player.getName().getString();
        String nick = player.getName().getString();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        String rankAndCallsign = rankDef.shortName + " " + callsign;
        int rankCW = font.width(rankAndCallsign);
        int nickW = font.width(nick);

        int donatColor = getDonateColor(ple.donatTier());
        String donatStr = getDonateLabel(ple.donatTier());
        boolean hasDonate = donatColor != 0;
        int donatW = hasDonate ? font.width(donatStr) + 2 : 0;

        int textW = Math.max(rankCW, nickW + donatW);
        int panelW = textW + PADDING * 2;
        int iconArea = ICON_SIZE + 2;
        if (panelW < iconArea + textW + PADDING) panelW = iconArea + textW + PADDING;
        int lineCount = 2;
        if (hasDonate) lineCount++;
        int panelH = lineCount * font.lineHeight + (lineCount - 1) * LINE_GAP + PADDING * 2;

        float scale = computeScale(dist);
        int centerX = 0;
        int panelX = -(panelW / 2);
        int panelY = -panelH;
        int panelZ = 0;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-scale, -scale, scale);

        int bgColor = ((int)(0xCC * alpha) << 24) | 0x0A0A0A;
        drawRect(poseStack, bufferSource, panelX, panelY, panelX + panelW, panelY + panelH, bgColor);

        int borderColor = ((int)(0x33 * alpha) << 24) | 0x808080;
        drawRect(poseStack, bufferSource, panelX, panelY, panelX + panelW, panelY + 1, borderColor);

        int textAlpha = (int)(0xFF * alpha);
        int primaryColor = (textAlpha << 24) | 0xFFFFFF;
        int secondaryColor = (textAlpha << 24) | 0xB0B0B0;
        int mutedColor = (textAlpha << 24) | 0x808080;

        int iy = panelY + PADDING + (font.lineHeight - ICON_SIZE) / 2;
        drawIcon(poseStack, panelX + PADDING, iy, rankDef, alpha);

        int cx = panelX + PADDING + ICON_SIZE + 2;
        font.drawInBatch(rankDef.shortName, cx, panelY + PADDING, rankDef.color & 0x00FFFFFF | (textAlpha << 24), true,
            poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        cx += font.width(rankDef.shortName) + 4;

        font.drawInBatch(callsign, cx, panelY + PADDING, primaryColor, true,
            poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

        int nickY = panelY + PADDING + font.lineHeight + LINE_GAP;
        font.drawInBatch(nick, panelX + PADDING, nickY, secondaryColor, true,
            poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

        if (hasDonate) {
            int donY = nickY + font.lineHeight + LINE_GAP;
            font.drawInBatch(donatStr, panelX + PADDING, donY, donatColor & 0x00FFFFFF | (textAlpha << 24), true,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

            if (ple.donatTier() == 3) {
                double pulse = Math.sin(System.currentTimeMillis() / 300.0) * 0.3 + 0.7;
                int glowA = (int)(pulse * 40 * alpha);
                if (glowA > 0) {
                    String full = "\u2B21 GENERAL";
                    int fw = font.width(full);
                    int glowColor = (glowA << 24) | (UITheme.DONATE_GENERAL & 0x00FFFFFF);
                    drawRect(poseStack, bufferSource, panelX + PADDING - 1, donY - 1,
                        panelX + PADDING + fw + 1, donY + font.lineHeight + 1, glowColor);
                }
            }
        }

        poseStack.popPose();
    }

    private static float calcAlpha(double dist) {
        if (dist <= 16) return 1.0f;
        if (dist >= MAX_DIST) return 0.1f;
        return (float)(1.0 - (dist - 16) / (MAX_DIST - 16) * 0.9);
    }

    private static float computeScale(double dist) {
        return 0.025f;
    }

    private static int getDonateColor(int donatTier) {
        return switch (donatTier) {
            case 1 -> UITheme.DONATE_VIP;
            case 2 -> UITheme.DONATE_ELITE_A;
            case 3 -> UITheme.DONATE_GENERAL;
            default -> 0;
        };
    }

    private static String getDonateLabel(int donatTier) {
        return switch (donatTier) {
            case 1 -> "VIP";
            case 2 -> "ELITE";
            case 3 -> "GENERAL";
            default -> "";
        };
    }

    private void drawIcon(PoseStack poseStack, int x, int y, RankDefinition rankDef, float alpha) {
        RenderSystem.setShaderTexture(0, rankDef.iconTexture);
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float u0 = 0f;
        float v0 = rankDef.getIconVOffset() / 160f;
        float u1 = 1f;
        float v1 = (rankDef.getIconVOffset() + 16) / 160f;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(poseStack.last().pose(), x, y + ICON_SIZE, 0).uv(u0, v1).endVertex();
        buffer.vertex(poseStack.last().pose(), x + ICON_SIZE, y + ICON_SIZE, 0).uv(u1, v1).endVertex();
        buffer.vertex(poseStack.last().pose(), x + ICON_SIZE, y, 0).uv(u1, v0).endVertex();
        buffer.vertex(poseStack.last().pose(), x, y, 0).uv(u0, v0).endVertex();
        tessellator.end();
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
