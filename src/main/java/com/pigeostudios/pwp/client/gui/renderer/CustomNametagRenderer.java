package com.pigeostudios.pwp.client.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.PlayerListEntry;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.core.Rank;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class CustomNametagRenderer {

    private static final double MAX_DIST = 48.0;
    private static final double FADE_START = 32.0;

    public void renderNametag(PoseStack poseStack, MultiBufferSource bufferSource,
                                Player player, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player localPlayer = mc.player;
        if (localPlayer == null) return;

        PlayerListEntry entry = ClientTeamData.playerDataMap.get(player.getUUID());
        if (entry == null) return;

        Team localTeam = ClientTeamData.getLocalPlayerTeam();
        Team playerTeam = Team.fromOrdinal(entry.teamOrdinal());

        if (localTeam == Team.SPECTATOR || playerTeam == Team.SPECTATOR) return;
        if (localTeam != playerTeam) return;

        double dist = player.distanceTo(localPlayer);
        if (dist > MAX_DIST) return;

        float alpha = 1f;
        if (dist > FADE_START) {
            alpha = (float) (1.0 - (dist - FADE_START) / (MAX_DIST - FADE_START));
        }

        Font font = mc.font;
        Rank rank = Rank.fromOrdinal(entry.rank());
        String rankPrefix = rank != null ? rank.getPrefix(false) : "";
        String callsign = entry.callsign();
        if (callsign == null || callsign.isEmpty()) {
            callsign = player.getName().getString();
        }
        String mcName = player.getName().getString();
        boolean showSecondLine = !mcName.equals(callsign);

        boolean hasDogTag = entry.hasReceivedDogTag();
        boolean isDonator = entry.donatTier() > 0;
        String donatorBadge = "\u2726";
        String displayName = hasDogTag ? "\u25C6 " + callsign : callsign;

        String badgePrefix = isDonator ? donatorBadge + " " : "";
        String topText = badgePrefix + (rankPrefix.isEmpty() ? displayName : rankPrefix + " " + displayName);
        String bottomText = showSecondLine ? "§7" + mcName : null;

        int tw = font.width(topText);
        int bw = bottomText != null ? font.width(bottomText) : 0;
        int maxW = Math.max(tw, bw);

        int padX = 6;
        int padY = 3;
        int lineGap = 1;
        int totalW = maxW + padX * 2;
        int totalH = padY * 2 + font.lineHeight;
        if (bottomText != null) {
            totalH += lineGap + font.lineHeight;
        }

        int teamColor = playerTeam == Team.NATO ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;

        int borderW = 1;

        poseStack.pushPose();
        poseStack.translate(0, player.getBbHeight() + 0.55, 0);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        float hw = totalW / 2f;
        float hh = totalH / 2f;
        float x1 = -hw;
        float y1 = -hh;
        float x2 = hw;
        float y2 = hh;

        // Background via gui RenderType (batched through bufferSource)
        VertexConsumer vc = bufferSource.getBuffer(RenderType.gui());
        Matrix4f mat = poseStack.last().pose();

        drawRect(vc, mat, x1, y1, x2, y2, applyAlpha(0x660A0A0A, alpha));

        // Team color frame with pulse
        float pulse = 0.7f + 0.3f * Mth.sin((float)(System.currentTimeMillis() / 800.0 * Math.PI * 2));
        int borderColor = applyAlpha(teamColor, alpha * pulse);
        drawRect(vc, mat, x1, y1, x2, y1 + borderW, borderColor);
        drawRect(vc, mat, x1, y2 - borderW, x2, y2, borderColor);
        drawRect(vc, mat, x1, y1, x1 + borderW, y2, borderColor);
        drawRect(vc, mat, x2 - borderW, y1, x2, y2, borderColor);

        // Text
        int textColor = applyAlpha(0xAAEFEFEF, alpha);
        int rankColor = applyAlpha(0xAAE07B00, alpha);

        float textY = -hh + padY;

        if (isDonator || !rankPrefix.isEmpty()) {
            float lx = -hw + padX;

            if (isDonator) {
                bw = font.width(donatorBadge);
                font.drawInBatch(donatorBadge, lx, textY,
                    applyAlpha(rainbowColor(), alpha), true,
                    poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
                lx += bw + 2;
            }

            if (!rankPrefix.isEmpty()) {
                int rw = font.width(rankPrefix);
                font.drawInBatch(rankPrefix, lx, textY,
                    rankColor, true,
                    poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
                lx += rw + 2;
            }

            font.drawInBatch(displayName, lx, textY,
                textColor, true,
                poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, 0xF000F0);
        } else {
            font.drawInBatch(displayName,
                -font.width(displayName) / 2f, textY,
                textColor, true,
                poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }

        if (bottomText != null) {
            font.drawInBatch(bottomText,
                -font.width(bottomText) / 2f, textY + font.lineHeight + lineGap,
                applyAlpha(0xAAA0A0A0, alpha), true,
                poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }

        poseStack.popPose();
    }

    private void drawRect(VertexConsumer vc, Matrix4f mat,
                          float x1, float y1, float x2, float y2, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        vc.vertex(mat, x1, y2, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x2, y2, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x2, y1, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x1, y1, 0).color(r, g, b, a).endVertex();
    }

    private static int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (Math.min(0xFF, Math.max(0, a)) << 24) | (color & 0x00FFFFFF);
    }

    private static int rainbowColor() {
        float hue = (float)((System.currentTimeMillis() % 3000) / 3000.0);
        return hsvToRgb(hue, 0.9f, 1.0f);
    }

    private static int hsvToRgb(float hue, float sat, float val) {
        float h = (hue - (float)Math.floor(hue)) * 6f;
        int i = (int)h;
        float f = h - i;
        float p = val * (1f - sat);
        float q = val * (1f - sat * f);
        float t = val * (1f - sat * (1f - f));
        float r, g, b;
        switch (i) {
            case 0: r = val; g = t; b = p; break;
            case 1: r = q; g = val; b = p; break;
            case 2: r = p; g = val; b = t; break;
            case 3: r = p; g = q; b = val; break;
            case 4: r = t; g = p; b = val; break;
            default: r = val; g = p; b = q; break;
        }
        return (0xFF << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }
}
